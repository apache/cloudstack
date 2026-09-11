#!/usr/bin/env python3

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent


def find_java_files():
    return ROOT.glob("**/*.java")


def extract_parameter_blocks(source):
    blocks = []
    start = 0

    while True:
        match = source.find("@Parameter(", start)
        if match == -1:
            break

        depth = 0
        in_string = False
        escape = False
        end = None

        for i in range(match, len(source)):
            char = source[i]

            if in_string:
                if escape:
                    escape = False
                elif char == "\\":
                    escape = True
                elif char == '"':
                    in_string = False
                continue

            if char == '"':
                in_string = True
            elif char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    end = i + 1
                    break

        if end is None:
            break

        blocks.append((match, source[match:end]))
        start = end

    return blocks


def is_string_parameter(block):
    return bool(
        re.search(
            r"\btype\s*=\s*CommandType\.STRING\b",
            block,
        )
    )


def extract_description(block):
    match = re.search(
        r"\bdescription\s*=\s*((?:\"(?:\\.|[^\"\\])*\")"
        r"(?:\s*\+\s*\"(?:\\.|[^\"\\])*\")*)",
        block,
        re.DOTALL,
    )

    if not match:
        return ""

    expression = match.group(1)

    strings = re.findall(
        r'"((?:\\.|[^"\\])*)"',
        expression,
        re.DOTALL,
    )

    return "".join(strings)


def extract_valid_values(description):
    patterns = [
        r"\bvalid\s+values?\s*(?:are|:)\s*(.+?)(?:\.|$)",
        r"\bvalid\s+options?\s*(?:are|:)\s*(.+?)(?:\.|$)",
        r"\bpossible\s+values?\s*(?:are|:|include)\s*(.+?)(?:\.|$)",
        r"\bpossible\s+options?\s*(?:are|:|include)\s*(.+?)(?:\.|$)",
        r"\ballowed\s+values?\s*(?:are|:)\s*(.+?)(?:\.|$)",
    ]

    match = None

    for pattern in patterns:
        match = re.search(pattern, description, re.IGNORECASE)
        if match:
            break

    if not match:
        return []

    values_text = match.group(1).strip()

    # Some descriptions contain a fixed set of examples but also
    # explicitly allow additional values. These are not closed enums
    # and therefore should not require an allowedValues annotation.
    if re.search(r"\bor\s+valid\s+protocol\s+number\b", values_text, re.IGNORECASE):
        return []

    # Remove a leading colon if the wording leaves one behind.
    values_text = values_text.lstrip(":").strip()

    # Convert "A, B, and C" into "A, B, C".
    values_text = re.sub(
        r",?\s+and\s+",
        ", ",
        values_text,
        flags=re.IGNORECASE,
    )

    values = [
        value.strip().strip('"').strip("'")
        for value in values_text.split(",")
    ]

    return [value for value in values if value]


def extract_parameter_name(block):
    match = re.search(
        r"\bname\s*=\s*(?:ApiConstants\.([A-Z0-9_]+)|\"([^\"]+)\")",
        block,
    )

    if not match:
        return "<unknown>"

    if match.group(1):
        return f"ApiConstants.{match.group(1)}"

    return match.group(2)

def extract_inner_enums(source):
    enums = {}

    pattern = re.compile(
        r"\benum\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{([^}]*)\}",
        re.DOTALL,
    )

    for match in pattern.finditer(source):
        enum_name = match.group(1)
        body = match.group(2)

        constants = []

        for constant in body.split(","):
            constant = constant.strip()

            constant_match = re.match(
                r"([A-Z][A-Z0-9_]*)\b",
                constant,
            )

            if constant_match:
                constants.append(constant_match.group(1))

        if constants:
            enums[enum_name] = constants

    return enums


def extract_parameter_field_name(source, parameter_end):
    match = re.match(
        r"\s*private\s+String\s+([A-Za-z_][A-Za-z0-9_]*)\s*;",
        source[parameter_end:],
    )

    if not match:
        return None

    return match.group(1)


def find_enum_for_parameter(source, parameter_end, field_name):
    if not field_name:
        return None

    enum_pattern = re.compile(
        rf"\benum\s+{re.escape(field_name.capitalize())}Values\b"
        r"\s*\{",
    )

    if enum_pattern.search(source):
        return field_name.capitalize() + "Values"

    return None

def check_file(path):
    source = Path(path).read_text(encoding="utf-8")
    violations = []

    enums = extract_inner_enums(source)

    for start, block in extract_parameter_blocks(source):
        if not is_string_parameter(block):
            continue

        if re.search(r"\ballowedValues\s*=", block):
            continue

        if re.search(r"\ballowedValueType\s*=", block):
            continue

        parameter_end = start + len(block)
        field_name = extract_parameter_field_name(
            source,
            parameter_end,
        )

        enum_name = find_enum_for_parameter(
            source,
            parameter_end,
            field_name,
        )

        if enum_name and enum_name in enums:
            line = source[:start].count("\n") + 1
            name = extract_parameter_name(block)

            violations.append(
                (
                    str(path),
                    line,
                    enums[enum_name],
                    name,
                )
            )

            continue

        description = extract_description(block)

        if not description:
            continue

        values = extract_valid_values(description)

        if not values:
            continue

        line = source[:start].count("\n") + 1
        name = extract_parameter_name(block)

        violations.append(
            (str(path), line, values, name)
        )

    return violations

def main():
    violations = []

    for path in find_java_files():
        violations.extend(check_file(path))

    if violations:
        print("Missing allowedValues annotations:")

        for path, line, values, name in violations:
            print(
                f"{path}:{line}: "
                f"parameter '{name}' specifies valid values "
                f"{values} but has no allowedValues annotation"
            )

        return 1

    print("No missing allowedValues annotations found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
