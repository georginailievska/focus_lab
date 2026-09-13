package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.focuslab.dto.CommentRequest;
import mk.focuslab.dto.FileDownload;
import mk.focuslab.dto.PostRequest;
import mk.focuslab.dto.PostResponse;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.AttachmentBlob;
import mk.focuslab.model.Post;
import mk.focuslab.model.PostAttachment;
import mk.focuslab.model.PostComment;
import mk.focuslab.model.Role;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.PostAttachmentRepository;
import mk.focuslab.repository.PostCommentRepository;
import mk.focuslab.repository.PostRepository;
import mk.focuslab.repository.SubjectRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedService {
    /** Колку објави враќа една страница од лентата. */
    private static final int PAGE_SIZE = 20;

    public static final int MAX_FILES = 4;
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, Boolean> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("image/png", true),
            Map.entry("image/jpeg", true),
            Map.entry("image/jpg", true),
            Map.entry("image/webp", true),
            Map.entry("image/gif", true),
            Map.entry("application/pdf", false),
            Map.entry("text/plain", false),
            Map.entry("text/markdown", false),
            Map.entry("application/zip", false),
            Map.entry("application/msword", false),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", false),
            Map.entry("application/vnd.ms-excel", false),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", false),
            Map.entry("application/vnd.ms-powerpoint", false),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", false)
    );

    /** Кој смее да пишува во лентата. Admin е тука само за модерација. */
    private static final Set<Role> CAN_POST = Set.of(Role.STUDENT, Role.MENTOR);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostAttachmentRepository attachmentRepository;
    private final SubjectRepository subjectRepository;
    private final DtoMapper mapper;

    // ---- читање -----------------------------------------------------------

    public List<PostResponse> listFeed(Long beforeId, Long subjectId) {
        List<Post> posts = postRepository.findFeed(
                beforeId == null ? PostRepository.NEWEST_FIRST : beforeId,
                subjectId == null ? PostRepository.ANY_SUBJECT : subjectId,
                Limit.of(PAGE_SIZE)
        );

        return posts.stream().map(mapper::toPostResponse).toList();
    }

    /** Бајтите на еден прилог — ова го служи отворената рута. */
    public FileDownload attachment(String accessKey) {
        PostAttachment attachment = attachmentRepository.findWithBlobByAccessKey(accessKey)
                .orElseThrow(() -> new ResourceNotFoundException("Датотеката не постои."));

        return new FileDownload(
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.isImage(),
                attachment.getBlob().getData()
        );
    }

    // ---- пишување ---------------------------------------------------------

    @Transactional
    public PostResponse createPost(User author, PostRequest request, List<MultipartFile> files) {
        requireCanPost(author);

        Post post = Post.builder()
                .author(author)
                .subject(request.subjectId() == null ? null : findSubjectOrThrow(request.subjectId()))
                .text(request.text().trim())
                .build();

        attach(post, files);
        postRepository.save(post);

        log.info("Корисник {} објави во лентата ({} прилози)", author.getId(), post.getAttachments().size());

        return mapper.toPostResponse(post);
    }

    @Transactional
    public PostResponse addComment(Long postId, User author, CommentRequest request) {
        requireCanPost(author);

        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Објавата не постои."));

        PostComment comment = PostComment.builder()
                .post(post)
                .author(author)
                .text(request.text().trim())
                .build();

        commentRepository.save(comment);
        post.getComments().add(comment);

        return mapper.toPostResponse(post);
    }

    /** Своја објава — или админ, како модератор. */
    @Transactional
    public void deletePost(Long postId, User actor) {
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Објавата не постои."));

        requireOwnerOrAdmin(post.getAuthor(), actor, "објавата");

        // Коментарите и прилозите одат со неа (cascade + orphanRemoval)
        postRepository.delete(post);
    }

    @Transactional
    public void deleteComment(Long commentId, User actor) {
        PostComment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Коментарот не постои."));

        requireOwnerOrAdmin(comment.getAuthor(), actor, "коментарот");

        comment.getPost().getComments().remove(comment);
        commentRepository.delete(comment);
    }

    // ---- прилози ----------------------------------------------------------

    private void attach(Post post, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<MultipartFile> actual = files.stream().filter(file -> !file.isEmpty()).toList();

        if (actual.size() > MAX_FILES) {
            throw new IllegalArgumentException("Најмногу " + MAX_FILES + " датотеки по објава.");
        }

        for (MultipartFile file : actual) {
            post.getAttachments().add(toAttachment(post, file));
        }
    }

    private PostAttachment toAttachment(Post post, MultipartFile file) {
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(
                    "Датотеката „" + safeName(file) + "\u201C е поголема од 5 MB.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();

        Boolean isImage = ALLOWED_TYPES.get(contentType);
        if (isImage == null) {
            throw new IllegalArgumentException(
                    "Тип на датотека што не е дозволен: " + safeName(file)
                            + ". Дозволени се слики, PDF, текст, ZIP и Office документи.");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            log.warn("Не успеа читање на качена датотека: {}", e.getMessage());
            throw new IllegalArgumentException("Датотеката не можеше да се прочита.");
        }

        if (isImage) {
            requireRealImage(data, safeName(file));
        }

        PostAttachment attachment = PostAttachment.builder()
                .post(post)
                .accessKey(newAccessKey())
                .filename(safeName(file))
                .contentType(contentType)
                .sizeBytes(data.length)
                .image(isImage)
                .build();

        attachment.setBlob(AttachmentBlob.builder()
                .attachment(attachment)
                .data(data)
                .build());

        return attachment;
    }

    private void requireRealImage(byte[] data, String filename) {
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(data));
            if (decoded == null) {
                throw new IllegalArgumentException("Датотеката „" + filename + "\u201C не е валидна слика.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Датотеката „" + filename + "\u201C не можеше да се прочита.");
        }
    }

    private String safeName(MultipartFile file) {
        String raw = file.getOriginalFilename() == null ? "датотека" : file.getOriginalFilename();
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();

        if (name.isEmpty()) {
            name = "датотека";
        }

        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String newAccessKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ---- правила ----------------------------------------------------------

    private void requireCanPost(User user) {
        if (!CAN_POST.contains(user.getRole())) {
            throw new ForbiddenActionException("Во лентата пишуваат студенти и ментори.");
        }
    }

    private void requireOwnerOrAdmin(User owner, User actor, String what) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }

        if (!owner.getId().equals(actor.getId())) {
            throw new ForbiddenActionException("Можеш да избришеш само " + what + " што е твој.");
        }
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои предмет со ID " + subjectId));
    }
}
