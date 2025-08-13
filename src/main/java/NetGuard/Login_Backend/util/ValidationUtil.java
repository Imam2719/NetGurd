package NetGuard.Login_Backend.util;

import NetGuard.Login_Backend.dto.RegisterRequest;
import NetGuard.Login_Backend.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9\\s()-]{8,20}$");

    public void validateRegistrationRequest(RegisterRequest request) {
        validateEmail(request.getEmail());
        validatePhone(request.getPhone());
        validatePassword(request.getPassword());
        validateAge(request.getAge());
        validateName(request.getName());
    }

    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email address is required");
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Please provide a valid email address");
        }

        if (email.length() > 150) {
            throw new ValidationException("Email address is too long");
        }
    }

    public void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone number is required");
        }

        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Please provide a valid phone number");
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }

        if (password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters long");
        }

        if (password.length() > 12) {
            throw new ValidationException("Password must not exceed 12 characters");
        }
    }

    public void validateAge(Integer age) {
        if (age == null) {
            throw new ValidationException("Age is required");
        }

        if (age < 18) {
            throw new ValidationException("Must be at least 18 years old");
        }

        if (age > 120) {
            throw new ValidationException("Please provide a realistic age");
        }
    }

    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        if (name.trim().length() < 2) {
            throw new ValidationException("Name must be at least 2 characters long");
        }

        if (name.length() > 100) {
            throw new ValidationException("Name is too long");
        }

        if (!name.matches("^[a-zA-Z\\s'-]+$")) {
            throw new ValidationException("Name can only contain letters, spaces, hyphens, and apostrophes");
        }
    }
}