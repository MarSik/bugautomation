# Bug automation tool

[![Build Status](https://travis-ci.org/MarSik/bugautomation.svg?branch=master)](https://travis-ci.org/MarSik/bugautomation)

This server will monitor multiple bug sources and keep them in sync using drools rules.

It currently supports bugzilla and Trello.

## Usage instructions

Create a configuration properties file config.properties in a similar way to the example below:

```
trello.appkey=<app key>
trello.token=<token>
trello.boards=<board id1>,<board id2>

bugzilla.url=https://bugzilla.redhat.com
bugzilla.owners=<comma separated list of bugzilla account emails>
bugzilla.teams=<comma separated list of teams>

bugzilla.username=<username>
bugzilla.password=<password>

# User mapping, repeat for each user (each user has different someid)
user.<someid>.bugzilla=<bugzilla account email>
user.<someid>.trello=<trello user id>

# Case sensitive name of the main trello board
cfg.board.sprint=Sprint
# The name of the backlog column in sprint board
cfg.backlog=todo

# Release (target milestone) score configuration
release.X.Y.Z=200

# Release mapping
release.map.X.Y.Z-beta=X.Y.Z
release.map.A.B.C=A.B.D

# Future release (wildcard map)
release.future.prefix=A.B.
release.future.release=A.B.0
release.A.B.0=100

```

Unpack the archive and start the server form the unpacked directory using:

```
java -cp '*' -Dbug.config=path/to/config.properties org.marsik.bugautomation.Main
```

