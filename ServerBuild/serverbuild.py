#!/usr/bin/python

from optparse import OptionParser
from sys import exit
import subprocess
import logging
import shlex
import os

BASE_URL = 'http://emberdb.com/downloads/'
INSTALLATION_DIR = '/opt/'
FILES_TO_DOWNLOAD = [
    'apache-cassandra-1.2.0-bin.tar.gz',
    'rexster-server-2.2.0.zip',
    'jdk-7u11-linux-x64.tar.gz'
]


def all():
    install()
    start()


def install():
    logging.debug('Running install')

    for file in FILES_TO_DOWNLOAD:
        downloadFile(file)
        extractFile(file)


def start():
    logging.debug('Running start')


def uninstall():
    logging.debug('Running uninstall')


def downloadFile(filename):
    logging.debug('Downloading %s' % filename)

    command = shlex.split('sudo wget %s%s' % (BASE_URL, filename))
    subprocess.check_call(command)


def extractFile(filename):
    logging.debug('Extracting %s' % filename)

    command = 'sudo '
    if '.tar.gz' in filename:
        command += 'tar xzf %s' % filename
    elif '.zip' in filename:
        command += 'unzip %s' % filename
    elif '.tar.bz2' in filename:
        command += 'tar xjf %s' % filename
    else:
        logging.critical('Unknown archive format, unable to extract %s' % filename)
        exit()

    command = shlex.split(command)
    subprocess.check_call(command)

    logging.debug('Deleting original archive of %s' % filename)

    command = shlex.split('sudo rm %s' % filename)
    subprocess.check_call(command)


if __name__ == '__main__':
    # Configure logging interface
    logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.DEBUG)

    # Create the arguments parser
    parser = OptionParser()
    parser.add_option('-i', '--install', action='store_true', dest='install',
        help='Download and install all required components')
    parser.add_option('-s', '--start', action='store_true', dest='start',
        help='Start all components (assumes already downloaded')
    parser.add_option('-a', '--all', action='store_true', dest='all',
        help='Download, install, and start all required components')
    parser.add_option('-u', '--uninstall', action='store_true', dest='uninstall',
        help='Remove all downloaded components')
    (options, args) = parser.parse_args()

    # Verify option combinations are valid
    if options.install and options.uninstall:
        logging.error('Cannot install and uninstall simultaneously')
    elif (options.install and options.all) or (options.start and options.all):
        logging.warning('--all replaces --install and --start')

    # Move to the installation directory
    os.chdir(INSTALLATION_DIR)

    # Start the program
    if options.all or (options.install and options.start):
        all()
    elif options.install:
        install()
    elif options.start:
        start()
    elif options.uninstall:
        uninstall()