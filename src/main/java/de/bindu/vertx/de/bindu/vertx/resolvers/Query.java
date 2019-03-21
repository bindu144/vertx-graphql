package de.bindu.vertx.de.bindu.vertx.resolvers;

import com.coxautodev.graphql.tools.GraphQLRootResolver;

import de.bindu.vertx.de.bindu.vertx.models.Link;
import de.bindu.vertx.de.bindu.vertx.models.LinkRepository;

import java.util.List;

public class Query implements GraphQLRootResolver
{

  private final LinkRepository linkRepository;

  public Query(LinkRepository linkRepository) {
    this.linkRepository = linkRepository;
  }

  public List<Link> allLinks() {
    return linkRepository.getAllLinks();
  }
}