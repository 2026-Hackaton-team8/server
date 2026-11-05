package org.study.jibeo.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.user.dto.MeResponse;
import org.study.jibeo.domain.user.dto.UpdateMeRequest;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MeResponse getMe(Long userId) {
        return MeResponse.from(findUser(userId));
    }

    @Transactional
    public MeResponse updateMe(Long userId, UpdateMeRequest request) {
        User user = findUser(userId);
        user.updateContact(request.name(), request.phone(), blankToNull(request.email()));
        return MeResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findWithCompanyById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
