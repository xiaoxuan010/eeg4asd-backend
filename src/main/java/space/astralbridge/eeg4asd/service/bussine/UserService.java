package space.astralbridge.eeg4asd.service.bussine;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import space.astralbridge.eeg4asd.dto.common.UserBasicInfo;
import space.astralbridge.eeg4asd.dto.request.GetUserRequestDTO;
import space.astralbridge.eeg4asd.dto.request.PostUserParentRequestDTO;
import space.astralbridge.eeg4asd.dto.request.PostUserRoleRequestDTO;
import space.astralbridge.eeg4asd.dto.request.UserChangePasswordPostRequestDTO;
import space.astralbridge.eeg4asd.dto.response.GetUserResponseDTO;
import space.astralbridge.eeg4asd.model.User;
import space.astralbridge.eeg4asd.repository.UserRepository;
import space.astralbridge.eeg4asd.service.auth.UserAuthService;
import space.astralbridge.eeg4asd.service.common.UserManagementService;
import space.astralbridge.eeg4asd.service.exception.UserLoginUserNotExistException;
import space.astralbridge.eeg4asd.service.exception.UserLoginUserOrPwdErrorException;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserService {
    private final UserRepository userRepository;
    private final UserAuthService userAuthService;
    private final UserManagementService userManagementService;

    public GetUserResponseDTO getUserBasicInfo(GetUserRequestDTO requestDTO, User authUser) {
        String role = authUser.getRole();
        User user = userRepository.findBy_id(requestDTO.getId());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if ("b".equals(user.getRole()) || "a".equals(role) || "b".equals(role)
                || authUser.get_id().equals(requestDTO.getId())) {
            return new GetUserResponseDTO(user.getUsername(), user.getRole(), user.getParentId(), user.getLegalName(),
                    user.getPhoneNumber());
        } else {
            throw new IllegalArgumentException("Permission denied");
        }
    }

    public GetUserResponseDTO getDefaultUserInfo(User user) {
        return new GetUserResponseDTO(user.getUsername(), user.getRole(), user.getParentId(), user.getLegalName(),
                user.getPhoneNumber());
    }

    public GetUserResponseDTO getUserInfo(GetUserRequestDTO requestDTO) {
        User user = userRepository.findBy_id(requestDTO.getId());
        return new GetUserResponseDTO(user.getUsername(), user.getRole(), user.getParentId(), user.getLegalName(),
                user.getPhoneNumber());
    }

    public void setUserRole(PostUserRoleRequestDTO requestDTO, User authUser) {
        if (requestDTO.getId() == null || requestDTO.getId().isBlank()) {
            requestDTO.setId(authUser.get_id());
        }
        User user = userRepository.findBy_id(requestDTO.getId());

        if (user.getRole() == null || user.getRole().isBlank() || "undefined".equals(user.getRole())
                || authUser.getRole().equals("a")) {
            if (!"a".equals(authUser.getRole()) && "a".equals(requestDTO.getRole())) {
                throw new IllegalArgumentException("Permission denied");
            } else {
                user.setRole(requestDTO.getRole());
                userRepository.save(user);
            }
        } else {
            throw new IllegalArgumentException("User role already set");
        }
        return;
    }

    public void setParent(PostUserParentRequestDTO requestDTO, User authUser) {
        if (requestDTO.getId() == null) {
            requestDTO.setId(authUser.get_id());
        }

        User user = userRepository.findBy_id(requestDTO.getId());

        if (user.getParentId() != null) {
            throw new IllegalArgumentException("User parent already set");
        } else if (user.getRole() == null) {
            throw new IllegalArgumentException("User role not set");
        } else if (!"c".equals(user.getRole())) {
            throw new IllegalArgumentException("Only customers can have parents");
        } else {
            User parent = userRepository.findBy_id(requestDTO.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("Parent not found");
            } else if (!"b".equals(parent.getRole())) {
                throw new IllegalArgumentException("Parent is not a business");
            } else {
                user.setParentId(requestDTO.getParentId());
                userRepository.save(user);
            }
        }
        return;
    }

    public List<UserBasicInfo> getChildrenList(User authUser) {
        if (authUser.getRole() == null || authUser.getRole().isBlank() || "undefined".equals(authUser.getRole())) {
            throw new IllegalArgumentException("User role not set");
        } else if (!"b".equals(authUser.getRole())) {
            throw new IllegalArgumentException("Only businesses can have children");
        } else {
            return userRepository.findByParentId(authUser.get_id())
                    .stream()
                    .map(UserBasicInfo::new)
                    .collect(Collectors.toList());
        }
    }

    public void changePassword(UserChangePasswordPostRequestDTO requestDTO, User authUser)
            throws InvalidKeyException, NoSuchAlgorithmException {
        String uid = authUser.get_id();
        String oldPwd = requestDTO.getOldPwd();
        String newPwd = requestDTO.getNewPwd();

        User user = userRepository.findBy_id(uid);
        if (user == null) {
            throw new UserLoginUserNotExistException(uid);
        }

        if (userAuthService.userPwdAuth(user.getUsername(), oldPwd) == null) {
            throw new UserLoginUserOrPwdErrorException();
        }

        userManagementService.changePassword(user, newPwd);
    }

}
