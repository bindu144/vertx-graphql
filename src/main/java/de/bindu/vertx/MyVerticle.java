package de.bindu.vertx;

import com.coxautodev.graphql.tools.SchemaParser;

import de.bindu.vertx.de.bindu.vertx.models.LinkRepository;
import de.bindu.vertx.de.bindu.vertx.resolvers.Query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.GraphQLSchema;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

public class MyVerticle extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(MyVerticle.class);

  GraphQL graphQL;

  public MyVerticle() {
    getGraphQL();
  }
  @Override
  public void start() {
    Router router = Router.router(vertx);
    router.route("/").handler(rc -> {
      rc.request().bodyHandler(rh -> {
        String query = rh.toString();
        handleQuery(rc, query);
      });
    });

    router.route("/browser").method(GET).handler(rc -> {
      if ("/browser".equals(rc.request().path())) {
        rc.response().setStatusCode(302);
        rc.response().headers().set("Location", rc.request().path() + "/");
        rc.response().end("You are in path!!");
      } else {
        rc.next();
      }
    });

    StaticHandler staticHandler = StaticHandler.create("graphiql");
    staticHandler.setDirectoryListing(false);
    staticHandler.setCachingEnabled(false);
    staticHandler.setIndexPage("index.html");
    router.route("/browser/*").method(GET).handler(staticHandler);


    vertx.createHttpServer().requestHandler(router).listen(8080);
  }

  public void getGraphQL()
  {
    GraphQLSchema graphQLSchema = buildSchema();
    this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
  }

  private GraphQLSchema buildSchema() {
    LinkRepository linkRepository = new LinkRepository();
    return SchemaParser.newParser()
                       .file("schema.graphqls")
                       .resolvers(new Query(linkRepository))
                       .build()
                       .makeExecutableSchema();
  }


  /**
   * Handle the graphql query.
   *
   * @param rc
   * @param json
   */
  private void handleQuery(RoutingContext rc, String json) {
    log.info("Handling query {" + json + "}");
    // The graphql query is transmitted within a JSON string
    JsonObject queryJson = new JsonObject(json);
    String query = queryJson.getString("query");


    ExecutionInput input = new ExecutionInput(query, null, queryJson, null, extractVariables(queryJson));

    ExecutionResult result = graphQL.execute(input);
    List<GraphQLError> errors = result.getErrors();
    JsonObject response = new JsonObject();
    // Check whether the query has returned any errors. We need to add those to the response as well.
    if (!errors.isEmpty()) {
      log.error("Could not execute query {" + query + "}");
      JsonArray jsonErrors = new JsonArray();
      response.put("errors", jsonErrors);
      for (GraphQLError error : errors) {
        if(error instanceof ExceptionWhileDataFetching) {
          ((ExceptionWhileDataFetching)error).getException().printStackTrace();
        }
        JsonObject jsonError = new JsonObject();
        jsonError.put("message", error.getMessage());
        jsonError.put("type", error.getErrorType());
        if (error.getLocations() != null && !error.getLocations().isEmpty()) {
          JsonArray errorLocations = new JsonArray();
          jsonError.put("locations", errorLocations);
          for (SourceLocation location : error.getLocations()) {
            JsonObject errorLocation = new JsonObject();
            errorLocation.put("line", location.getLine());
            errorLocation.put("column", location.getLine());
            errorLocations.add(errorLocation);
          }
        }
        jsonErrors.add(jsonError);
      }
    }
    if (result.getData() != null) {
      Map<String, Object> data = (Map<String, Object>) result.getData();
      response.put("data", new JsonObject(Json.encode(data)));
    }
    HttpResponseStatus statusCode = (result.getErrors() != null && !result.getErrors().isEmpty()) ? BAD_REQUEST : OK;

    rc.response().putHeader("Content-Type", "application/json");
    rc.response().setStatusCode(statusCode.code());
    rc.response().end(response.toString());

  }

  /**
   * Extracts the variables of a query as a map. Returns empty map if no variables are found.
   *
   * @param request
   *            The request body
   * @return GraphQL variables
   */
  private Map<String, Object> extractVariables(JsonObject request) {
    JsonObject variables = request.getJsonObject("variables");
    if (variables == null) {
      return Collections.emptyMap();
    } else {
      return variables.getMap();
    }
  }

}
