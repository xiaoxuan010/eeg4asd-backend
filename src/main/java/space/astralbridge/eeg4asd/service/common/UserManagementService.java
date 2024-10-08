package space.astralbridge.eeg4asd.service.common;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import space.astralbridge.eeg4asd.model.User;
import space.astralbridge.eeg4asd.repository.UserRepository;
import space.astralbridge.eeg4asd.service.exception.UserRegisterUsernameExistsException;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserManagementService {
    private final UserRepository userRepository;

    private final KeyManagementService keyManagementService;

    public boolean isUsernameValid(String username) {
        return userRepository.findByUsername(username) == null;
    }

    public User createUser(String username, String password, String role, String legalName, String phoneNumber)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (!isUsernameValid(username)) {
            throw new UserRegisterUsernameExistsException(username);
        }

        SecretKey secretKey = keyManagementService.generateHmacKey();
        byte[] pwdSecretKey = secretKey.getEncoded();
        byte[] pwdHash = keyManagementService.HmacSHA256(password, secretKey);

        User user = new User();
        user.setUsername(username);
        user.setPwdSecretKey(pwdSecretKey);
        user.setPassword(pwdHash);
        user.setRole(role);
        user.setLegalName(legalName);
        user.setPhoneNumber(phoneNumber);

        userRepository.save(user);
        return user;
    }

    public void changePassword(User user, String newPassword) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = keyManagementService.generateHmacKey();
        byte[] pwdSecretKey = secretKey.getEncoded();
        byte[] pwdHash = keyManagementService.HmacSHA256(newPassword, secretKey);

        user.setPwdSecretKey(pwdSecretKey);
        user.setPassword(pwdHash);

        userRepository.save(user);
    }

}
