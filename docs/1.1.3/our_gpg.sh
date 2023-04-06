#!/bin/bash 
 echo $FOO_PASSPHRASE | /usr/bin/gpg --batch --no-tty --passphrase-fd 0 "$@"
