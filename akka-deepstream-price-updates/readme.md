### Akka and Deepstream together

A simple example repository showing integration with Akka and Deepstream, this repository consists of a front end interface with a Deepstream client and a back end streaming mock financial data via Akka to Deepstream.

In production, at its simplest it could look as follows:

![price-streaming-architecture](price-streaming-architecture.png)

Each customer has a deepstream client in their browser which connects to a load balanced deepstream cluster. Behind the deepstreams we have a few things:

1. A cluster of Akka nodes providing price updates to the Deepstream nodes. The actual updates are sent via a normal Deepstream client using `listening`, a concept where updates only need to be provided when we have interested clients. With the front end we're able to indicate which prices we're interested in dynamically and this will trigger events in

The another autoscaling group of deepstream microservices. Not only do these provide any admin functionality in the application (such as an RPC call “purchase-item”), they can also aggregate any data statistics or metrics. These microservices then pump data into a message queue which existing Akka nodes are able to pick up and process.

The benefit of having deepstream clients in the browser for users is that you can add and remove functionality at runtime. This means new features can come online without your customers even having to refresh their browsers. Additionally it completely decouples backend development work while seamlessly integrating with existing services.
