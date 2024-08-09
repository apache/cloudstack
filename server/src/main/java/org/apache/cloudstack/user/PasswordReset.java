package org.apache.cloudstack.user;

import com.cloud.user.UserAccount;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface PasswordReset {
    ConfigKey<Long> PasswordResetTtl = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "password.reset.ttl", "30",
            "Password reset ttl in minutes", true, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetEmailSender = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "password.reset.email.sender", null,
            "Password reset email sender", true, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPHost = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "password.reset.smtp.host", null,
            "Password reset smtp host", false, ConfigKey.Scope.Global);

    ConfigKey<Integer> PasswordResetSMTPPort = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "password.reset.smtp.port", "25",
            "Password reset smtp port", false, ConfigKey.Scope.Global);

    ConfigKey<Boolean> PasswordResetSMTPUseAuth = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Boolean.class, "password.reset.smtp.useAuth", "false",
            "Use auth for smtp in Password reset", false, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPUsername = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "password.reset.smtp.username", null,
            "Password reset smtp username", false, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPPassword = new ConfigKey<>("Secure", String.class,
            "password.reset.smtp.password", null,
            "Password reset smtp password", false, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetMailTemplate = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "password.reset.mail.template", "Hello {{username}}!\n" +
            "You have requested to reset your password. Please click the following link to reset your password:\n" +
            "{{{reset_link}}}\n" +
            "If you did not request a password reset, please ignore this email.\n" +
            "\n" +
            "Regards,\n" +
            "The CloudStack Team",
            "Password reset mail template. This uses mustache template engine. Available " +
                    "variables are: username, firstName, lastName, resetLink, token",
            true,
            ConfigKey.Scope.Global);

    void setResetTokenAndSend(UserAccount userAccount);

    boolean validateAndResetPassword(UserAccount user, String token, String password);
}
