package de.bindu.vertx;

import io.vertx.core.Vertx;

public class GraphQLServer
{
    public static void main(String[] args)
    {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(new MyVerticle());
    }
}