# SqlBrowserFX

SqlBrowserFX is a feature rich sql client for SQLite , MySQL.
[Click here to download](https://gitlab.com/Paris_Kolovos/sqlbrowser/raw/development/builds/sqlbrowserfx-latest.tar.gz) for both windows and linux 

![](images/sqlbrowserfx.png)

Flat Light Theme

![](images/sqlbrowserfx2.png)

### Features

* Manage data (insert, update, delete) via gui.
* Execute raw sql queries.
* Editor for sql with syntax highlighting, autocomplete features.
* Adjustable responsive ui.
* Graphical representation of database as tree.
* Exposure of database to the web as RESTful service with one click.
* Import, export csv files.
* Support for SQLite.
* Basic support for MySQL.
* Cross Platform.


### Prerequisites

* JDK 8 with JavaFX like oracle jdk 8 or zulufx 8.
* MySQL server for usage with mysql.

### Installing

Import the project to your favorite ide as maven project and run SQlBrowserFXApp class.


### Build standalone app

Run build.sh script, this will generate all files needed in 'dist' folder.
Run sql-browser.exe for Windows, or run sqlbrowser.sh for Linux.


## Awesome projects used

* [DockFX](https://github.com/RobertBColton/DockFX) - The docking framework used (a moded version actually)
* [RichTextFΧ](https://github.com/FXMisc/RichTextFX) - Library which provides editor with syntax highlighting feature.
* [Spark](https://github.com/perwendel/spark)  - The web framework used




