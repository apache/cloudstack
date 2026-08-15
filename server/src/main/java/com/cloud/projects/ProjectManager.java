// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.projects;

import java.util.List;

import com.cloud.user.Account;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface ProjectManager extends ProjectService {
    public static final ConfigKey<Boolean> ProjectSmtpUseStartTLS = new ConfigKey<Boolean>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "project.smtp.useStartTLS", "false",
            "If set to true and if we enable security via project.smtp.useAuth, this will enable StartTLS to secure the connection.", true);

    public static final ConfigKey<String> ProjectSmtpEnabledSecurityProtocols = new ConfigKey<String>(ConfigKey.CATEGORY_ADVANCED, String.class, "project.smtp.enabledSecurityProtocols", "",
            "White-space separated security protocols; ex: \"TLSv1 TLSv1.1\". Supported protocols: SSLv2Hello, SSLv3, TLSv1, TLSv1.1 and TLSv1.2", true, ConfigKey.Kind.WhitespaceSeparatedListWithOptions, "SSLv2Hello,SSLv3,TLSv1,TLSv1.1,TLSv1.2");

    public static final ConfigKey<Boolean> ProjectSmtpUseAuth = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "project.smtp.useAuth", "false",
            "If true, use SMTP authentication when sending emails", false, ConfigKey.Scope.ManagementServer);

    ConfigKey<Boolean> ProjectInviteRequired = new ConfigKey<>("Project Defaults", Boolean.class,
            "project.invite.required", "false",
            "If invitation confirmation is required when add account to project. Default value is false", true);

    ConfigKey<Long> ProjectInvitationExpirationTime = new ConfigKey<>("Project Defaults", Long.class,
            "project.invite.timeout", "86400",
            "Invitation expiration time (in seconds). Default is 1 day - 86400 seconds", true);

    ConfigKey<Boolean> AllowUserToCreateProject = new ConfigKey<>("Project Defaults", Boolean.class,
            "allow.user.create.projects", "true",
            "If regular user can create a project; true by default", true);

    ConfigKey<String> ProjectEmailSender = new ConfigKey<>("Project Defaults", String.class,
            "project.email.sender", null,
            "Sender of project invitation email (will be in the From header of the email)", true);

    ConfigKey<String> ProjectSMTPHost = new ConfigKey<>("Project Defaults", String.class,
            "project.smtp.host", null,
            "SMTP hostname used for sending out email project invitations", true);

    ConfigKey<Integer> ProjectSMTPPort = new ConfigKey<>("Project Defaults", Integer.class,
            "project.smtp.port", "465",
            "Port the SMTP server is listening on", true);

    ConfigKey<String> ProjectSMTPUsername = new ConfigKey<>("Project Defaults", String.class,
            "project.smtp.username", null,
            "Username for SMTP authentication (applies only if project.smtp.useAuth is true)", true);

    ConfigKey<String> ProjectSMTPPassword = new ConfigKey<>("Secure", String.class,
            "project.smtp.password", null,
            "Password for SMTP authentication (applies only if project.smtp.useAuth is true)", true);

    boolean canAccessProjectAccount(Account caller, long accountId);

    boolean canModifyProjectAccount(Account caller, long accountId);

    boolean deleteAccountFromProject(long projectId, Account account);

    List<Long> listPermittedProjectAccounts(long accountId);

    boolean projectInviteRequired();

    boolean allowUserToCreateProject();

    boolean deleteProject(Account caller, long callerUserId, ProjectVO project);

    long getInvitationTimeout();

    public static final String MESSAGE_CREATE_TUNGSTEN_PROJECT_EVENT = "Message.CreateTungstenProject.Event";
    public static final String MESSAGE_DELETE_TUNGSTEN_PROJECT_EVENT = "Message.DeleteTungstenProject.Event";

}
