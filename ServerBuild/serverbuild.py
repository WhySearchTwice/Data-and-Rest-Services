#!/usr/bin/python

from optparse import OptionParser
from sys import exit
import subprocess
import logging
import shlex
import time
import sys
import os

BASE_URL = 'https://s3.amazonaws.com/why-search-twice'
INSTALLATION_DIR = '/opt'
FILES_TO_DOWNLOAD = [
    {'zipped': 'apache-cassandra-1.2.0-bin.tar.gz', 'unzipped': 'apache-cassandra-1.2.0'},
    {'zipped': 'rexster-server-2.2.0.zip', 'unzipped': 'rexster-server-2.2.0'},
    {'zipped': 'jdk-7u11-linux-x64.tar.gz', 'unzipped': 'jdk1.7.0_11'}
]


def all():
    install()


def install():
    logging.debug('Running install')

    logging.debug('Installing unzip')
    executeCommand('sudo apt-get install unzip')

    for file in FILES_TO_DOWNLOAD:
        downloadFile(file)

    # Replace Cassandra configuration file
    logging.debug('Installing Cassandra configuration file')
    os.chdir('/opt/%s/conf' % FILES_TO_DOWNLOAD[0]['unzipped'])
    executeCommand('sudo rm cassandra.yaml')
    executeCommand('sudo wget %s/cassandra.yaml' % BASE_URL)

    # Create Cassandra symlink
    executeCommand(
        'sudo ln -s %s/%s/bin/cassandra /usr/bin/cassandra' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[0]['unzipped']))

    # Replace the rexster configuration file
    logging.debug('Installing Rexster configuration file')
    os.chdir('%s/%s' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[1]['unzipped']))
    executeCommand('sudo rm rexster.xml')
    executeCommand('sudo wget %s/rexster.xml' % BASE_URL)

    # Create rexster symlink
    executeCommand(
        'sudo ln -s %s/%s/bin/rexster.sh /usr/bin/rexster' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[1]['unzipped']))

    # Install JDK
    logging.debug('Installing JDK')
    executeCommand('sudo ln -s %s/%s/bin/java /usr/bin/java' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[2]['unzipped']))
    executeCommand('sudo ln -s %s/%s/bin/javac /usr/bin/javac' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[2]['unzipped']))
    executeCommand('sudo ln -s %s/%s/bin/javah /usr/bin/javah' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[2]['unzipped']))

    start()

    # Initialize the indicies
    logging.debug('Initializing the indicies')
    executeCommand('curl --data "" http://localhost:8182/graphs/WhySearchTwice/keyindices/vertex/domain')
    executeCommand('curl --data "" http://localhost:8182/graphs/WhySearchTwice/keyindices/vertex/username')
	executeCommand('curl --data "" http://localhost:8182/graphs/WhySearchTwice/keyindices/vertex/userguid')
    executeCommand('curl --data "" http://localhost:8182/graphs/WhySearchTwice/keyindices/vertex/pageOpenTime')


def start():
    logging.debug('Running start')

    # Start Cassandra
    os.chdir('%s/%s/bin' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[0]['unzipped']))
    executeCommand('sudo ./cassandra', fireAndForget=True)

    logging.info('Waiting a few seconds to allow Cassandra to start...')
    time.sleep(10)
    logging.info('Waiting complete, continuing')

    # Start rexster
    os.chdir('%s/%s' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[1]['unzipped']))
    executeCommand('sudo ./bin/rexster.sh -s &', fireAndForget=True)

    logging.info('Processes started')

    logging.info('Waiting a few seconds to allow Rexter to start...')
    time.sleep(10)
    logging.info('Waiting complete, continuing')


def stop():
    logging.debug('Running stop')

    # Stop Rexster
    os.chdir('%s/%s' % (INSTALLATION_DIR, FILES_TO_DOWNLOAD[1]['unzipped']))
    executeCommand('sudo ./bin/rexster.sh -x', fireAndForget=True)

    logging.info('Waiting a few seconds to allow Rexster to stop...')
    time.sleep(3)
    logging.info('Waiting complete, continuing')

    # Stop Cassandra
    logging.warn(
        'Cassandra cannot be killed programmatically. Please use "ps aux | grep cassandra" and "sudo kill -9 <PID>"')


def uninstall():
    stop()

    logging.debug('Running uninstall')
    os.chdir(INSTALLATION_DIR)

    logging.debug('Deleting Cassandra')
    executeCommand('sudo rm -r %s' % FILES_TO_DOWNLOAD[0]['unzipped'])

    logging.debug('Deleting rexster')
    executeCommand('sudo rm -r %s' % FILES_TO_DOWNLOAD[1]['unzipped'])


def downloadFile(file):
    logging.debug('Downloading %s' % file['zipped'])

    if not os.path.exists('%s/%s' % (INSTALLATION_DIR, file['unzipped'])):
        executeCommand('sudo wget %s/%s' % (BASE_URL, file['zipped']))
        extractFile(file)
    else:
        logging.debug('File already exists')


def extractFile(file):
    logging.debug('Extracting %s' % file['zipped'])

    command = 'sudo '
    if '.tar.gz' in file['zipped']:
        command += 'tar xzf %s' % file['zipped']
    elif '.zip' in file['zipped']:
        command += 'unzip -q %s' % file['zipped']
    elif '.tar.bz2' in file['zipped']:
        command += 'tar xjf %s' % file['zipped']
    else:
        logging.critical('Unknown archive format, unable to extract %s' % file['zipped'])
        exit()

    executeCommand(command)

    logging.debug('Deleting original archive of %s' % file['zipped'])

    executeCommand('sudo rm %s' % file['zipped'])


def executeCommand(command, fireAndForget=False):
    splitCommand = shlex.split(command)
    if fireAndForget:
        subprocess.Popen(splitCommand)
    else:
        try:
            subprocess.check_output(splitCommand)
        except subprocess.CalledProcessError as e:
            logging.error('A command failed to run properly: %s' % command)
            print(e.output)


if __name__ == '__main__':
    # Configure logging interface
    logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.DEBUG)

    # Create the arguments parser
    parser = OptionParser()
    parser.add_option('-i', '--install', action='store_true', dest='install',
        help='Download and install all required components')
    parser.add_option('-s', '--start', action='store_true', dest='start',
        help='Start all components (assumes already downloaded)')
    parser.add_option('-x', '--stop', action='store_true', dest='stop',
        help='Stop all running components')
    parser.add_option('-a', '--all', action='store_true', dest='all',
        help='Download, install, and start all required components')
    parser.add_option('-u', '--uninstall', action='store_true', dest='uninstall',
        help='Remove all downloaded components')
    (options, args) = parser.parse_args()

    # Verify that the script is run as root
    if os.geteuid() != 0:
        logging.fatal('Please run as root')
        sys.exit(1)

    # Verify option combinations are valid
    if options.install and options.uninstall:
        logging.error('Cannot install and uninstall simultaneously')
    elif (options.install and options.all) or (options.start and options.all):
        logging.warning('--all replaces --install and --start')
    elif not options.all and not options.install and not options.start and not options.stop and not options.uninstall:
        parser.print_help()
        sys.exit(0)

    if (options.install):
        logging.info('Note: Install has an implied start to allow graph to initialize')

    # Move to the installation directory
    os.chdir(INSTALLATION_DIR)

    # Start the program
    if options.all or (options.install and options.start):
        all()
    elif options.install:
        install()
    elif options.start:
        start()
    elif options.stop:
        stop()
    elif options.uninstall:
        uninstall()
