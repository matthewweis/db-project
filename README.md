# Kansas Weather Map

Weather map of Kansas counties which displays government data alongside supplemental data entered by users.

## Getting Started

This project exists in two main parts: the database and the application. This github repo contains all code for the application. The database may be run in multiple ways, but instructions for running a database with Docker are included within logic/src/test/java/edu/dbgroup/logic/DBConnectionTest.java.

### Prerequisites

This project uses the Maven build system to manage dependencies, modules, etc. Simply clone the repo and open as a Maven project and all dependencies will be automatically downloaded and ready to go. The 
project can be run from any class containing a main() method, but gui/src/main/java/edu/dbgroup/gui/ApplicationLauncher.java is the main launch point.

### Installing

If you are familiar with Maven, just run "mvn install" and the front-end will work out of the box.

If you are unfamiliar with Maven, I highly recommend opening the project with IntelliJ which comes with Maven bundled.

```
Download IntelliJ at www.jetbrains.com/idea/download/
```

```
Open IntelliJ and select "Check out from Version Control" > "Git"
```

```
In the pop-up window enter the URL "https://github.com/MatthewWeisCaps/db-project.git" (without quotes) or "git@github.com:MatthewWeisCaps/db-project.git" (without quotes)
```

```
If IntelliJ asks if you would like to open the project, click "Yes"
```

```
IntelliJ will detect that a Maven project has been opened and display a pop-up in the lower right-hand corner of the screen which reads "Maven projects need to be imported." On the pop-up, click either "Import Changes" or "Enable Auto-Import" (which option you select doesn't matter unless you plan on changing build files).
```

These instructions only apply to the project's frontend. The database is a seperate entity.

## Running the tests

Currently the project includes a couple of tests in logic/src/test:

```
DBConnectionTest.java tests whether or not the application can connect to a given database. This test also includes instructions for creating a lightweight Microsoft SQL DB in Docker, although using Microsoft SQL Management Studio / Azure works as well.
```

```
DBQueryTest.java runs a few tests against a sample database to show rxjava2-jdbc is working. Included methods are very similar to those from rxjava2-jdbc's README.md and are likely irrelevant to those setting up the project.
```

## Built With

* [JavaFX](https://docs.oracle.com/javase/8/javafx/get-started-tutorial/jfx-overview.htm) - GUI Library
* [Maven](https://maven.apache.org/) - Dependency Management
* [RxJava](https://github.com/ReactiveX/RxJava) - Non-blocking reactive streams
* [Jdbc](https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html) - SQL Connection Library
* Other utility libraries such as Guava, rxjava2-jdbc, RxJavaFX, Slf4j+Logback, Microsoft SQL Jdbc drivers, etc. (see pom for all libs)

## Authors

* **Group 15** - *Created at K-State during Fall 2018 for CIS 560/562*

