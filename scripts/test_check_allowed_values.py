#!/usr/bin/env python3

import tempfile
import unittest
from pathlib import Path

from scripts import check_allowed_values


class CheckAllowedValuesTest(unittest.TestCase):

    def test_extract_valid_values(self):
        description = (
            "Provisioning type used to create volumes. "
            "Valid values are thin, sparse, fat."
        )

        self.assertEqual(
            check_allowed_values.extract_valid_values(description),
            ["thin", "sparse", "fat"],
        )

    def test_extract_valid_values_with_and(self):
        description = (
            "Valid values are HOURLY, DAILY, WEEKLY, and MONTHLY"
        )

        self.assertEqual(
            check_allowed_values.extract_valid_values(description),
            ["HOURLY", "DAILY", "WEEKLY", "MONTHLY"],
        )

    def test_open_ended_protocol_values_are_ignored(self):
        description = (
            "TCP/UDP/ICMP/ALL or valid protocol number"
        )

        self.assertEqual(
            check_allowed_values.extract_valid_values(description),
            [],
        )

    def test_open_ended_protocol_values_with_protocol_reference_are_ignored(self):
        description = (
            "TCP/UDP/ICMP/ALL or valid protocol number "
            "(see /etc/protocols)"
        )

        self.assertEqual(
            check_allowed_values.extract_valid_values(description),
            [],
        )

    def test_string_parameter_is_detected(self):
        block = """
        @Parameter(
            name = "test",
            type = CommandType.STRING,
            description = "Valid values are A, B"
        )
        """

        self.assertTrue(
            check_allowed_values.is_string_parameter(block)
        )

    def test_non_string_parameter_is_ignored(self):
        block = """
        @Parameter(
            name = "test",
            type = CommandType.INTEGER,
            description = "Valid values are 1, 2"
        )
        """

        self.assertFalse(
            check_allowed_values.is_string_parameter(block)
        )

    def test_parameter_with_allowed_values_is_not_reported(self):
        source = """
        @Parameter(
            name = "test",
            type = CommandType.STRING,
            allowedValues = {"A", "B"},
            description = "Valid values are A, B"
        )
        private String test;
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            self.assertEqual(
                check_allowed_values.check_file(path),
                [],
            )
        finally:
            path.unlink()

    def test_missing_allowed_values_is_reported(self):
        source = """
        @Parameter(
            name = "test",
            type = CommandType.STRING,
            description = "Valid values are A, B"
        )
        private String test;
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            violations = check_allowed_values.check_file(path)

            self.assertEqual(len(violations), 1)
            self.assertEqual(
                violations[0][2],
                ["A", "B"],
            )
        finally:
            path.unlink()

    def test_non_string_parameter_is_not_reported(self):
        source = """
        @Parameter(
            name = "test",
            type = CommandType.INTEGER,
            description = "Valid values are 1, 2"
        )
        private Integer test;
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            self.assertEqual(
                check_allowed_values.check_file(path),
                [],
            )
        finally:
            path.unlink()

    def test_inner_enum_is_extracted(self):
        source = """
        public class TestCmd {
            public enum RepairValues {
                LEAKS, ALL
            }
        }
        """

        enums = check_allowed_values.extract_inner_enums(source)

        self.assertEqual(
            enums,
            {
                "RepairValues": ["LEAKS", "ALL"],
            },
        )

    def test_enum_parameter_without_allowed_values_is_reported(self):
        source = """
        public class TestCmd {
            @Parameter(
                name = "repair",
                type = CommandType.STRING
            )
            private String repair;

            public enum RepairValues {
                LEAKS, ALL
            }
        }
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            violations = check_allowed_values.check_file(path)

            self.assertEqual(len(violations), 1)
            self.assertEqual(
                violations[0][2],
                ["LEAKS", "ALL"],
            )
        finally:
            path.unlink()

    def test_enum_parameter_with_allowed_values_is_not_reported(self):
        source = """
        public class TestCmd {
            @Parameter(
                name = "repair",
                type = CommandType.STRING,
                allowedValues = {"LEAKS", "ALL"}
            )
            private String repair;

            public enum RepairValues {
                LEAKS, ALL
            }
        }
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            self.assertEqual(
                check_allowed_values.check_file(path),
                [],
            )
        finally:
            path.unlink()

    def test_enum_parameter_with_allowed_value_type_is_not_reported(self):
        source = """
        public class TestCmd {
            @Parameter(
                name = "repair",
                type = CommandType.STRING,
                allowedValueType = RepairValues.class
            )
            private String repair;

            public enum RepairValues {
                LEAKS, ALL
            }
        }
        """

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=".java",
            encoding="utf-8",
            delete=False,
        ) as file:
            file.write(source)
            path = Path(file.name)

        try:
            self.assertEqual(
                check_allowed_values.check_file(path),
                [],
            )
        finally:
            path.unlink()


if __name__ == "__main__":
    unittest.main()
