# Readme for Simulation of Group 6
This project implements the solution of designing and building a
mobility model for the FMI building.

## File Structure
The following directory tree shows the files, which we modified
or created to make our simulation work.
the-one-group-6 <br>
├── data <br>
│ &nbsp; &nbsp; ├── FMI_building_v3.png <br>
│ &nbsp; &nbsp; └── fmi_locations.json <br>
├── example_settings <br>
│ &nbsp; &nbsp; └── spy_settings.txt <br>
├── src<br>
│ &nbsp; &nbsp; ├── movement<br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; ├── map <br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; │ &nbsp; &nbsp; └── UniGraph.java<br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; ├── UniMovement.java<br>
│ &nbsp; &nbsp; ├── report<br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; └── UniReport.java<br>
│ &nbsp; &nbsp; ├── routing<br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; ├── SpyCounterRouter.java<br>
│ &nbsp; &nbsp; │ &nbsp; &nbsp; └── SpyRouter.java<br>
└── └── util<br>
 &nbsp; &ensp; &nbsp; &ensp; &nbsp; &ensp; └── Agenda.java<br>


## External Libraries
Our implementation uses several external libraries. In the table
is an exhaustiv list of all required libraries to be imported into 
the project's structure.

| Library      | Version | Link                                                                                                                             |
|--------------|---------|----------------------------------------------------------------------------------------------------------------------------------|
| gson         | 2.10    | [link](https://repo.mavenlibs.com/maven/com/google/code/gson/gson/2.10/gson-2.10.jar?utm_source=mavenlibs.com)                   |
| jgrahpt-core | 1.4.0   | [link](https://repo1.maven.org/maven2/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar)                                     |
| json-simple  | 1.1     | [link](https://repo.mavenlibs.com/maven/com/googlecode/json-simple/json-simple/1.1/json-simple-1.1.jar?utm_source=mavenlibs.com) |
| jts-core     | 1.19.0  | [link](https://repo.mavenlibs.com/maven/org/locationtech/jts/jts-core/1.19.0/jts-core-1.19.0.jar?utm_source=mavenlibs.com)       |
| jheaps       | 0.13.0  | [link](https://repo.mavenlibs.com/maven/org/jheaps/jheaps/0.13/jheaps-0.13.jar?utm_source=mavenlibs.com)                         |

You can import these libraries to IntelliJ via 
`File -> Project Structure -> Project Settings -> Libraries`,
then hit the `+`-sign and `Java`, select the library and import it.
Alternatively, you can use maven to get all dependencies.

## Run the Simulation
To run the simulation, you only need to set the `example_settings/spy_settings.txt`
file as a program's arguments. Then simply run the simulator as usual
by starting `DTNSim`. For a more enjoyable experience activate the underlay.
