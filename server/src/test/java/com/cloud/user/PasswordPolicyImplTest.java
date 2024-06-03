//  Licensed to the Apache Software Foundation (ASF) under one or more
//  contributor license agreements.  See the NOTICE file distributed with
//  this work for additional information regarding copyright ownership.
//  The ASF licenses this file to You under the Apache License, Version 2.0
//  (the "License"); you may not use this file except in compliance with
//  the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package com.cloud.user;

import com.cloud.exception.InvalidParameterValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PasswordPolicyImplTest {

    @Spy
    private PasswordPolicyImpl passwordPolicySpy;

    @Test
    public void validateSpecialCharactersTestWithEnoughSpecialCharacters() {

        Mockito.doReturn(3).when(passwordPolicySpy).getPasswordPolicyMinimumSpecialCharacters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfSpecialCharacters(3, "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateSpecialCharactersTestWithoutEnoughSpecialCharacters() {
        Mockito.doReturn(4).when(passwordPolicySpy).getPasswordPolicyMinimumSpecialCharacters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfSpecialCharacters(3, "user", null);
    }

    @Test
    public void validateSpecialCharactersTestWithNoMinimumSpecialCharacters() {
        Mockito.doReturn(0).when(passwordPolicySpy).getPasswordPolicyMinimumSpecialCharacters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfSpecialCharacters(3, "user", null);
    }

    @Test
    public void validateUpperCaseLettersTestWithEnoughUpperCaseLetters() {
        Mockito.doReturn(3).when(passwordPolicySpy).getPasswordPolicyMinimumUpperCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfUpperCaseLetters(3, "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateUpperCaseLettersTestWithoutEnoughUpperCaseLetters() {
        Mockito.doReturn(4).when(passwordPolicySpy).getPasswordPolicyMinimumUpperCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfUpperCaseLetters(3, "user", null);
    }

    @Test
    public void validateUpperCaseLettersTestWithNoMinimumUpperCaseLetters() {
        Mockito.doReturn(0).when(passwordPolicySpy).getPasswordPolicyMinimumUpperCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfUpperCaseLetters(3, "user", null);
    }

    @Test
    public void validateLowerCaseLettersTestWithEnoughLowerCaseLetters() {
        Mockito.doReturn(3).when(passwordPolicySpy).getPasswordPolicyMinimumLowerCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfLowerCaseLetters(3, "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateLowerCaseLettersTestWithoutEnoughLowerCaseLetters() {
        Mockito.doReturn(4).when(passwordPolicySpy).getPasswordPolicyMinimumLowerCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfLowerCaseLetters(3, "user", null);
    }

    @Test
    public void validateLowerCaseLettersTestWithNoMinimumLowerCaseLetters() {
        Mockito.doReturn(0).when(passwordPolicySpy).getPasswordPolicyMinimumLowerCaseLetters(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfLowerCaseLetters(3, "user", null);
    }

    @Test
    public void validateDigitsTestWithEnoughDigits() {
        Mockito.doReturn(3).when(passwordPolicySpy).getPasswordPolicyMinimumDigits(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfDigits(3, "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateDigitsTestWithoutDigits() {
        Mockito.doReturn(4).when(passwordPolicySpy).getPasswordPolicyMinimumDigits(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfDigits(3, "user", null);
    }

    @Test
    public void validateDigitsTestWithNoMinimumDigits() {
        Mockito.doReturn(0).when(passwordPolicySpy).getPasswordPolicyMinimumDigits(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfDigits(3, "user", null);
    }

    @Test
    public void validateMinimumLengthTestWithEnoughMinimumLength() {
        Mockito.doReturn(5).when(passwordPolicySpy).getPasswordPolicyMinimumLength(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumLength("12345", "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateMinimumLengthTestWithoutMinimumLength() {
        Mockito.doReturn(5).when(passwordPolicySpy).getPasswordPolicyMinimumLength(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumLength("1234", "user", null);
    }

    @Test
    public void validateMinimumLengthTestWithNoMinimumLength() {
        Mockito.doReturn(0).when(passwordPolicySpy).getPasswordPolicyMinimumLength(null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumLength("123456", "user", null);
    }

    @Test
    public void validateUsernameInPasswordTestUsernameInPasswordAllowedAndWithoutUsernameInPassword() {
        Mockito.doReturn(true).when(passwordPolicySpy).getPasswordPolicyAllowPasswordToContainUsername(null);
        passwordPolicySpy.validateIfPasswordContainsTheUsername("123456", "user", null);
    }

    @Test
    public void validateUsernameInPasswordTestUsernameInPasswordAllowedAndWithUsernameInPassword() {
        Mockito.doReturn(true).when(passwordPolicySpy).getPasswordPolicyAllowPasswordToContainUsername(null);
        passwordPolicySpy.validateIfPasswordContainsTheUsername("user123", "user", null);
    }

    @Test
    public void validateUsernameInPasswordTestUsernameInPasswordNotAllowedAndWithoutUsernameInPassword() {
        Mockito.doReturn(false).when(passwordPolicySpy).getPasswordPolicyAllowPasswordToContainUsername(null);
        passwordPolicySpy.validateIfPasswordContainsTheUsername("123456", "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateUsernameInPasswordTestUsernameInPasswordNotAllowedAndWithUsernameInPassword() {
        Mockito.doReturn(false).when(passwordPolicySpy).getPasswordPolicyAllowPasswordToContainUsername(null);
        passwordPolicySpy.validateIfPasswordContainsTheUsername("user123", "user", null);
    }

    @Test
    public void validateRegexTestWithRegexAndComplyingPassword() {
        Mockito.doReturn("[a-z]+").when(passwordPolicySpy).getPasswordPolicyRegex(null);
        passwordPolicySpy.validateIfPasswordMatchesRegex("abcd", "user", null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateRegexTestWithRegexAndWithoutComplyingPassword() {
        Mockito.doReturn("[a-z]+").when(passwordPolicySpy).getPasswordPolicyRegex(null);
        passwordPolicySpy.validateIfPasswordMatchesRegex("abcd123", "user", null);
    }

    @Test
    public void validateRegexTestWithoutRegex() {
        Mockito.doReturn(null).when(passwordPolicySpy).getPasswordPolicyRegex(null);
        passwordPolicySpy.validateIfPasswordMatchesRegex("abcd123", "user", null);
    }

    @Test
    public void validateCombinationOfPolicies() {
        Mockito.doReturn(2).when(passwordPolicySpy).getPasswordPolicyMinimumSpecialCharacters(null);
        Mockito.doReturn(1).when(passwordPolicySpy).getPasswordPolicyMinimumUpperCaseLetters(null);
        Mockito.doReturn(1).when(passwordPolicySpy).getPasswordPolicyMinimumLowerCaseLetters(null);
        Mockito.doReturn(1).when(passwordPolicySpy).getPasswordPolicyMinimumDigits(null);
        Mockito.doReturn(8).when(passwordPolicySpy).getPasswordPolicyMinimumLength(null);
        Mockito.doReturn(false).when(passwordPolicySpy).getPasswordPolicyAllowPasswordToContainUsername(null);

        String password = "Ab1!@#cd";
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfSpecialCharacters(2, password, null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfUpperCaseLetters(1, password, null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfLowerCaseLetters(1, password, null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumNumberOfDigits(1, password, null);
        passwordPolicySpy.validateIfPasswordContainsTheMinimumLength(password, "user", null);
        passwordPolicySpy.validateIfPasswordContainsTheUsername(password, "user", null);
    }

}
