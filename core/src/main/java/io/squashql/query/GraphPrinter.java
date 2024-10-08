package io.squashql.query;

import io.squashql.query.database.QueryScope;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class GraphPrinter {

  public static <N> void print(DependencyGraph<N> graph) {
    StringBuilder sb = new StringBuilder();

    Set<DependencyGraph.NodeWithId<N>> roots = new HashSet<>();
    for (DependencyGraph.NodeWithId<N> node : graph.nodes()) {
      if (graph.inDegree(node) == 0) {
        roots.add(node);
      }
    }

    IntSupplier id = new AtomicInteger()::getAndIncrement;
    Map<QueryScope, Integer> idByScope = new HashMap<>();
    Function<QueryScope, Integer> idProvider = scope -> idByScope.computeIfAbsent(scope, k -> id.getAsInt());
    Set<N> alreadyPrinted = new HashSet<>();
    for (DependencyGraph.NodeWithId<N> root : roots) {
      appendNode(sb, root, alreadyPrinted, idProvider);
      sb.append(System.lineSeparator());
      executeRecursively(sb, graph, root, alreadyPrinted, idProvider, 1);
    }

    sb.append(System.lineSeparator());
    sb.append("Scopes:").append(System.lineSeparator());
    for (Map.Entry<QueryScope, Integer> e : idByScope.entrySet()) {
      sb.append("#").append(e.getValue()).append(": ").append(printQueryPlanNodeKey(e.getKey())).append(System.lineSeparator());
    }

    System.out.println(sb);
  }

  private static <N> void executeRecursively(StringBuilder sb, DependencyGraph<N> graph, DependencyGraph.NodeWithId<N> node, Set<N> alreadyPrinted, Function<QueryScope, Integer> idProvider, int level) {
    Set<DependencyGraph.NodeWithId<N>> successors = graph.successors(node);
    for (DependencyGraph.NodeWithId<N> successor : successors) {
      sb.append("\t".repeat(Math.max(0, level)));
      appendNode(sb, successor, alreadyPrinted, idProvider);
      sb.append(System.lineSeparator());
      executeRecursively(sb, graph, successor, alreadyPrinted, idProvider, level + 1);
    }
  }

  private static <N> void appendNode(StringBuilder sb, DependencyGraph.NodeWithId<N> node, Set<N> alreadyPrinted, Function<QueryScope, Integer> idProvider) {
    sb.append("#").append(node.id);
    if (alreadyPrinted.add(node.node)) {
      sb.append(", n=").append(printQueryPlanNodeKey(idProvider, node.node));
    }
  }

  private static String printQueryPlanNodeKey(Function<QueryScope, Integer> idProvider, Object o) {
    if (o instanceof QueryExecutor.QueryPlanNodeKey key) {
      StringBuilder sb = new StringBuilder();
      sb.append("scope=").append("#").append(idProvider.apply(key.queryScope()));
      sb.append(", measure=[").append(key.measure().alias()).append("]; [").append(key.measure()).append("]");
      return sb.toString();
    } else {
      return String.valueOf(o);
    }
  }

  private static String printQueryPlanNodeKey(QueryScope scope) {
    StringBuilder sb = new StringBuilder();
    sb.append(scope);
    return sb.toString();
  }
}
