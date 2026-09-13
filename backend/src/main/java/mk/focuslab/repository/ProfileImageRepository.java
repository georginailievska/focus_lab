package mk.focuslab.repository;

import mk.focuslab.model.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {
    @Query("select image from ProfileImage image where image.user.avatarKey = :key")
    Optional<ProfileImage> findByAvatarKey(@Param("key") String key);

    void deleteByUserId(Long userId);
}
