package mk.focuslab.mapper;

import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.AttachmentResponse;
import mk.focuslab.dto.AuthorResponse;
import mk.focuslab.dto.CommentResponse;
import mk.focuslab.dto.AuthResponse;
import mk.focuslab.dto.MentorNoteResponse;
import mk.focuslab.dto.MentorResponse;
import mk.focuslab.dto.NotificationResponse;
import mk.focuslab.dto.OverlapResponse;
import mk.focuslab.dto.PostResponse;
import mk.focuslab.dto.ProfileResponse;
import mk.focuslab.dto.ProfileStats;
import mk.focuslab.dto.SessionNoteResponse;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.dto.StudentCommentResponse;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.dto.UserResponse;
import mk.focuslab.model.Notification;
import mk.focuslab.model.Post;
import mk.focuslab.model.PostAttachment;
import mk.focuslab.model.PostComment;
import mk.focuslab.model.Session;
import mk.focuslab.model.SessionApplication;
import mk.focuslab.model.SessionNote;
import mk.focuslab.model.StudentComment;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DtoMapper {
    private static final String AVATAR_PATH = "/api/avatars/";

    public String avatarUrl(User user) {
        String key = user.getAvatarKey();
        return key == null ? null : AVATAR_PATH + key;
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getMentorStatus(),
                user.getCreatedAt(),
                avatarUrl(user)
        );
    }

    public List<UserResponse> toUserResponses(List<User> users) {
        return users.stream().map(this::toUserResponse).toList();
    }

    public MentorResponse toMentorResponse(User mentor) {
        return new MentorResponse(mentor.getId(), mentor.getFullName(), avatarUrl(mentor));
    }

    public OverlapResponse toOverlapResponse(Session session, boolean mine) {
        return new OverlapResponse(
                session.getId(),
                session.getTitle(),
                session.getStartTime(),
                session.getEndTime(),
                toSubjectResponse(session.getSubject()),
                session.getMentors().stream()
                        .map(this::toMentorResponse)
                        .sorted(Comparator.comparing(MentorResponse::fullName))
                        .toList(),
                mine
        );
    }

    public SubjectResponse toSubjectResponse(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }

    public AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getMentorStatus(),
                avatarUrl(user)
        );
    }

    public ProfileResponse toProfileResponse(
            User user,
            String visibleEmail,
            List<SubjectResponse> interests,
            ProfileStats stats
    ) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                visibleEmail,
                user.getRole(),
                user.getMentorStatus(),
                user.getCreatedAt(),
                avatarUrl(user),
                interests,
                stats
        );
    }

    /**
     * @param locationVisible линкот или салата се праќаат само на менторите на
     *                        сесијата, на прифатените студенти и на админ —
     *                        инаку секој најавен би можел да влезе на средбата.
     */
    public SessionResponse toSessionResponse(
            Session session,
            long applicantsCount,
            long approvedCount,
            boolean locationVisible
    ) {
        return new SessionResponse(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                toSubjectResponse(session.getSubject()),
                session.getMentors().stream()
                        .map(this::toMentorResponse)
                        .sorted(Comparator.comparing(MentorResponse::fullName))
                        .toList(),
                session.getMode(),
                locationVisible ? session.getLocation() : null,
                session.getStartTime(),
                session.getEndTime(),
                applicantsCount,
                session.getMaxApplicants(),
                approvedCount,
                session.getMaxApproved(),
                session.getTags().stream().sorted().toList()
        );
    }

    // ---- лента --------------------------------------------------------------

    private static final String ATTACHMENT_PATH = "/api/attachments/";

    /** Автор во лентата: без email — види коментарот на AuthorResponse. */
    public AuthorResponse toAuthorResponse(User user) {
        return new AuthorResponse(user.getId(), user.getFullName(), user.getRole(), avatarUrl(user));
    }

    public AttachmentResponse toAttachmentResponse(PostAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                ATTACHMENT_PATH + attachment.getAccessKey(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.isImage()
        );
    }

    public CommentResponse toCommentResponse(PostComment comment) {
        return new CommentResponse(
                comment.getId(),
                toAuthorResponse(comment.getAuthor()),
                comment.getText(),
                comment.getCreatedAt()
        );
    }

    public StudentCommentResponse toStudentCommentResponse(StudentComment comment) {
        return new StudentCommentResponse(
                comment.getId(),
                toAuthorResponse(comment.getAuthor()),
                comment.getText(),
                comment.isSharedWithMentors(),
                comment.getCreatedAt()
        );
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getSessionId(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }

    public MentorNoteResponse toMentorNoteResponse(SessionNote note) {
        Session session = note.getSession();

        return new MentorNoteResponse(
                note.getId(),
                toAuthorResponse(note.getAuthor()),
                note.getText(),
                note.getCreatedAt(),
                session.getId(),
                session.getTitle(),
                session.getStartTime(),
                toSubjectResponse(session.getSubject())
        );
    }

    public SessionNoteResponse toSessionNoteResponse(SessionNote note) {
        return new SessionNoteResponse(
                note.getId(),
                toAuthorResponse(note.getAuthor()),
                note.getText(),
                note.getCreatedAt()
        );
    }

    public PostResponse toPostResponse(Post post) {
        return new PostResponse(
                post.getId(),
                toAuthorResponse(post.getAuthor()),
                post.getSubject() == null ? null : toSubjectResponse(post.getSubject()),
                post.getText(),
                post.getAttachments().stream().map(this::toAttachmentResponse).toList(),
                post.getComments().stream().map(this::toCommentResponse).toList(),
                post.getCreatedAt()
        );
    }

    public ApplicationResponse toApplicationResponse(SessionApplication application) {
        Session session = application.getSession();

        return new ApplicationResponse(
                application.getId(),
                toUserResponse(application.getStudent()),
                session.getId(),
                session.getTitle(),
                session.getStartTime(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getDecidedAt()
        );
    }

    public List<ApplicationResponse> toApplicationResponses(List<SessionApplication> applications) {
        return applications.stream().map(this::toApplicationResponse).toList();
    }
}
