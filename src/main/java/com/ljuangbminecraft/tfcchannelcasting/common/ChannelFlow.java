package com.ljuangbminecraft.tfcchannelcasting.common;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.LOGGER;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.TFCCCBlockEntities;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ChannelBlock;
import com.ljuangbminecraft.tfcchannelcasting.common.blocks.MoldBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ChannelFlow 
{
    public static ChannelFlow fromSource(Level level, BlockPos originChannel)
    {
        int counter = 0;
        final BiMap<Integer, BlockPos> channelsAndMolds = HashBiMap.create();

        // Only channels are keys to this map, but the list of neighbors
        // contains both channel and mold
        final Map<BlockPos, List<BlockPos>> neighbors = new HashMap<>();

        final Deque<BlockPos> pendingVisit = new ArrayDeque<>();
        final Set<BlockPos> molds = new HashSet<>();

        pendingVisit.add(originChannel);
        while (pendingVisit.size() > 0)
        {
            BlockPos current = pendingVisit.pop();
            if (channelsAndMolds.containsValue(current)) continue;

            channelsAndMolds.put(counter, current);
            counter++;

            // Find adjacent channels and molds
            List<BlockPos> adjacentChannels = findAdjacent(level, current, false);
            List<BlockPos> adjacentMolds = findAdjacent(level, current, true);

            // Channels should be visited in the future so that
            // the entire net of channels is found
            adjacentChannels.stream().forEach(adj-> pendingVisit.add(adj));

            // Save molds for later
            molds.addAll(adjacentMolds);

            // The neighbors of the channel are both the channel and mold
            neighbors.put(current, adjacentChannels);
            neighbors.get(current).addAll(adjacentMolds);

            
            // LOGGER.debug("Visting: " + current);
            // LOGGER.debug("  Pending visit: " + pendingVisit.stream().map(a->a.toString()).collect(Collectors.toList()));
            // LOGGER.debug("  Visited: " + channels.values().stream().map(a->a.toString()).collect(Collectors.toList()));
        }

        // Add a numeric index to the molds
        for (BlockPos mold : molds)
        {
            channelsAndMolds.put(counter, mold);
            counter++;
        }

        LOGGER.debug(channelsAndMolds.values().stream().map(a->a.toString()).collect(Collectors.toList()).toString());
        LOGGER.debug(neighbors.entrySet().stream().map(a->a.getKey().toString() + " " + a.getValue().toString()).collect(Collectors.toList()).toString());

        int[][] graph = new int[channelsAndMolds.size()][channelsAndMolds.size()];
        double[][] heuristic = new double[channelsAndMolds.size()][channelsAndMolds.size()];

        // channel are the x of the graph adjacency matrix, while neighborChannelOrMold
        // are the y. This means that all channels are two-way connected to each other
        // (flow can go in both directions), while channels are connected to molds in
        // one-way (flow can only go INTO the mold)
        // This means that the flow from source to mold will
        // never cross another mold in the net.
        for (BlockPos channel : channelsAndMolds.values())
        {
            if (!neighbors.containsKey(channel)) continue; // Molds dont have neighbors

            for (BlockPos neighborChannelOrMold : neighbors.get(channel))
            {
                int x = channelsAndMolds.inverse().get(channel); 
                int y = channelsAndMolds.inverse().get(neighborChannelOrMold);
                graph[x][y] = 1;
                if (heuristic[x][y] == 0)
                {
                    heuristic[x][y] = heuristic[y][x] = Math.sqrt( channel.distSqr(neighborChannelOrMold) );
                }
            }
        }

        final Map<BlockPos, List<BlockPos>> paths = new HashMap<BlockPos, List<BlockPos>>();

        for (BlockPos mold : molds)
        {
            // This is a path of BlockPos from the originChannel (the one adjacent to the crucible)
            // to the BlockPos of the mold (not included)
            LOGGER.debug("MoldBlock at " + mold);
            ArrayDeque<BlockPos> path = aStar(
                graph, 
                heuristic, 
                0,
                channelsAndMolds.inverse().get(mold)
            ).stream()
            .map(channelsAndMolds::get) // index to BlockPos
            .collect( // Inverts the order because the aStar returns a list goal -> start
                Collector.of(
                    ArrayDeque::new,
                    (deq, t) -> deq.addFirst(t),
                    (d1, d2) -> { d2.addAll(d1); return d2; }
                )
            );
            paths.put(mold, new ArrayList<>(path));

            for (BlockPos p : path)
            {
                LOGGER.debug("    " + p);
            }
        }

        return new ChannelFlow(paths);
    }

    private static List<BlockPos> findAdjacent(Level level, BlockPos current, boolean findMolds)
    {
        final List<BlockPos> adjacent = new ArrayList<>();
        for (Direction dir : Direction.values())
        {
            if (dir == Direction.UP) continue;

            BlockPos relative = current.relative(dir);
            Block block = level.getBlockState(relative).getBlock();
            if (findMolds)
            {
                if (block instanceof MoldBlock)
                {
                    adjacent.add(relative);
                }
            }
            else
            {
                if (block instanceof ChannelBlock)
                {
                    adjacent.add(relative);
                }
            }
            
        }
        return adjacent;
    }

    /**
     * Finds the shortest distance between two nodes using the A-star algorithm
     * @param graph an adjacency-matrix-representation of the graph where (x,y) is the weight of the edge or 0 if there is no edge.
     * @param heuristic an estimation of distance from node x to y that is guaranteed to be lower than the actual distance. E.g. straight-line distance
     * @param start the node to start from.
     * @param goal the node we're searching for
     * @return The path from goal to start (not included)
     * 
     * Adapted from https://github.com/ClaasM/Algorithms/blob/master/src/a_star/java/simple/AStar.java
     * */
    private static List<Integer> aStar(int[][] graph, double[][] heuristic, int start, int goal) {

        //This contains the distances from the start node to all other nodes
        int[] distances = new int[graph.length];
        //Initializing with a distance of "Infinity"
        Arrays.fill(distances, Integer.MAX_VALUE);
        //The distance from the start node to itself is of course 0
        distances[start] = 0;

        int[] parent = new int[graph.length];

        //This contains the priorities with which to visit the nodes, calculated using the heuristic.
        double[] priorities = new double[graph.length];
        //Initializing with a priority of "Infinity"
        Arrays.fill(priorities, Integer.MAX_VALUE);
        //start node has a priority equal to straight line distance to goal. It will be the first to be expanded.
        priorities[start] = heuristic[start][goal];

        //This contains whether a node was already visited
        boolean[] visited = new boolean[graph.length];

        //While there are nodes left to visit...
        while (true) {

            // ... find the node with the currently lowest priority...
            double lowestPriority = Integer.MAX_VALUE;
            int lowestPriorityIndex = -1;
            for (int i = 0; i < priorities.length; i++) {
                //... by going through all nodes that haven't been visited yet
                if (priorities[i] < lowestPriority && !visited[i]) {
                    lowestPriority = priorities[i];
                    lowestPriorityIndex = i;
                }
            }

            if (lowestPriorityIndex == -1) {
                // There was no node not yet visited --> Node not found
                throw new IllegalArgumentException("Illegal graph! No connection between start and end.");
            } else if (lowestPriorityIndex == goal) {
                // Goal node found
                ArrayList<Integer> finalPath = new ArrayList<>();
                int currentIndex = lowestPriorityIndex;
                while (currentIndex != start)
                {
                    finalPath.add(currentIndex);
                    currentIndex = parent[currentIndex];
                }
                return finalPath;
            }

            //...then, for all neighboring nodes that haven't been visited yet....
            for (int i = 0; i < graph[lowestPriorityIndex].length; i++) {
                if (graph[lowestPriorityIndex][i] != 0 && !visited[i]) {
                    //...if the path over this edge is shorter...
                    if (distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i] < distances[i]) {
                        //...save this path as new shortest path
                        distances[i] = distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i];
                        parent[i] = lowestPriorityIndex;
                        //...and set the priority with which we should continue with this node
                        priorities[i] = distances[i] + heuristic[i][goal];
                    }
                }
            }

            // Lastly, note that we are finished with this node.
            visited[lowestPriorityIndex] = true;
        }
    }

    private Map<BlockPos, List<BlockPos>> paths;
    
    protected ChannelFlow(Map<BlockPos, List<BlockPos>> paths) {
        this.paths = paths;
    }

    public Stream<MoldBlockEntity> getMolds(Level level)
    {
        prunePaths(level);
        return paths.keySet().stream().map(
            pos -> level.getBlockEntity(pos, TFCCCBlockEntities.MOLD_TABLE.get()).get()
        );
    }

    // Remove molds from the flow whose path is invalid 
    // (the mold is no longer present or the channel is no longer present)
    private void prunePaths(Level level)
    {
        paths.entrySet().removeIf(
            e -> {
                Optional<MoldBlockEntity> mold = level.getBlockEntity(e.getKey(), TFCCCBlockEntities.MOLD_TABLE.get());

                // MoldBlockEntity is no longer present
                if (mold.isEmpty()) return true; 

                // MoldBlockEntity already has an output, cannot be filled more
                if (!mold.get().getOutputStack().isEmpty()) return true;

                // The path of channels has been broken
                if (!e.getValue().stream().allMatch(
                    ch -> level.getBlockState(ch).getBlock() instanceof ChannelBlock
                )) return true;

                return false;
            }
        );
    }

    public boolean isFlowFinished()
    {
        return paths.isEmpty();
    }

    public void removeMold(BlockPos moldPos)
    {
        paths.remove(moldPos);
    }
}
