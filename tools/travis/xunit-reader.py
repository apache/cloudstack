# !/usr/bin/env python
# encoding: utf-8

"""
This script provides parsing of xunit xml files.

"""

import os
import argparse

import texttable

import lxml.etree


def main():
    """
    Entry point for the parser

    """

    args = _generate_args()
    file_path_list = _generate_file_list(args)

    exit(parse_reports(file_path_list))


def _generate_args():
    parser = argparse.ArgumentParser(
        description='Command line utility for reading xunit xml files'
    )

    parser.add_argument(
        'path',
        metavar='/path/to/folder/containing/xunit-reports',
        type=str,
        help='A path to a folder containing xunit reports'
    )

    args = parser.parse_args()

    return vars(args)


def _generate_file_list(args):
    path = args.pop('path')
    file_path_list = []
    for (root, dirnames, filenames) in os.walk(path):
        for filename in filenames:
            if filename.endswith('.xml'):
                file_path_list.append(os.path.join(root, filename))

    return file_path_list


def parse_reports(file_path_list):
    table = texttable.Texttable()
    table.header(['Test', 'Result'])

    exit_code = 0

    for file_path in file_path_list:
        data = lxml.etree.iterparse(file_path, tag='testcase')
        for event, elem in data:
            name = ''
            status = 'Success'
            if 'name' in elem.attrib:
                name = elem.attrib['name']
            for children in elem.getchildren():
                if 'skipped' == children.tag:
                    status = 'Skipped'
                elif 'failure' == children.tag:
                    exit_code = 1
                    status = 'Failure'

            table.add_row([name, status])

    print table.draw()

    return exit_code


if __name__ == "__main__":
    main()