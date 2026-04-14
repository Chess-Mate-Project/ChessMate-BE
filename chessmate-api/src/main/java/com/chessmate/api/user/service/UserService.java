package com.chessmate.api.user.service;

import com.chessmate.api.image.ImageUtil;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final LichessUserRepository lichessUserRepository;
    private final ChesscomUserRepository chesscomUserRepository;
    private final ImageUtil imageUtil;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId, OAuthPlatForm platform) {
        return switch (platform) {
            case LICHESS -> {
                LichessUser user = lichessUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                yield new ProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    OAuthPlatForm.LICHESS,
                    user.getDescription(),
                    imageUtil.getProfileImageUrl(user.getId(), user.getProfile()),
                    imageUtil.getBannerImageUrl(user.getId(), user.getBanner()),
                    user.getCreatedAt(),
                    user.getPlatformJoinedAt()
                );
            }
            case CHESSCOM -> {
                ChesscomUser user = chesscomUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                yield new ProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    OAuthPlatForm.CHESSCOM,
                    user.getDescription(),
                    imageUtil.getProfileImageUrl(user.getId(), user.getProfile()),
                    imageUtil.getBannerImageUrl(user.getId(), user.getBanner()),
                    user.getCreatedAt(),
                    user.getPlatformJoinedAt()
                );
            }
        };
    }

    @Transactional
    public void updateDescription(Long userId, OAuthPlatForm platform, String description) {
        switch (platform) {
            case LICHESS -> {
                LichessUser user = lichessUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                user.setDescription(description);
                lichessUserRepository.save(user);
            }
            case CHESSCOM -> {
                ChesscomUser user = chesscomUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                user.setDescription(description);
                chesscomUserRepository.save(user);
            }
        }
    }
}
