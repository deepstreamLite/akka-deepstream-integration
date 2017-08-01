### Akka and Deepstream together

A simple example repository showing integration with Akka and Deepstream, this repository consists of a front end interface with a Deepstream client and some simple back end micro services built with Akka and Deepstream. One of the micro-services provides (mock) financial data while the other handles purchases of stocks. In reality there are many different services you could run other than these, such as admin functionality or metrics collection.

In production, at its simplest it could look as follows:

![price-streaming-architecture](price-streaming-architecture.png)

Each customer has a deepstream client in their browser which connects to a load balanced deepstream cluster. Behind the Deepstreams we have two micro service clusters:

1. A cluster of Akka nodes providing price updates to the Deepstreams. The actual updates are sent via a normal Deepstream client using `listening`, a concept where updates only need to be provided when we have interested clients. With the front end we're able to indicate which prices we're interested in dynamically so that only these will be sent

2. A cluster of Akka nodes handling purchases of stocks. To showcase failure and success scenarios, every second purchase will fail with a red flash of the row. Each successful purchase will give a green flash.

### How to run

##### Deepstream

The first thing that needs to start in this architecture is the Deepstream server. You can download this off npm, clone it from GitHub or download the binary. For simplicity we'll be downloading it from npm.

```
\> npm install -g deepstream.io
\> deepstream
      _                     _
   __| | ___  ___ _ __  ___| |_ _ __ ___  __ _ _ __ ____
  / _` |/ _ \/ _ \ '_ \/ __| __| '__/ _ \/ _` | '_ ` _  \
 | (_| |  __/  __/ |_) \__ \ |_| | |  __/ (_| | | | | | |
  \__,_|\___|\___| .__/|___/\__|_|  \___|\__,_|_| |_| |_|
                 |_|
 =====================   starting   ======================
```

##### Back end

The back end micro services were built using Scala and as such you should be able to run them using sbt as follows:

```
\> git clone git@github.com:deepstreamIO/akka-deepstream-integration.git
\> cd akka-deepstream-integration
\> ./sbt
...
...
...
...\> run
```

##### Front end

The web interface is self contained and relies on CDNs for dependencies. As such you should just be able to open the `index.html` file.