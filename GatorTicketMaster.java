import java.io.*;
import java.util.*;

public class GatorTicketMaster {
    // Data structures to store reserved seats, waitlist, available seats, user mapping, and assigned seats
    private RedBlackTree reservedSeats;  // store reserved seat info
    private PriorityQueue<User> waitlist;  //users on the waitlist
    private PriorityQueue<Integer> availableSeats;  //available seats
    private Map<Integer, User> userMap;  // store user
    private Set<Integer> assignedSeats; // Track assigned seats

    // Constructor: initialize the system and allocate seats
    public GatorTicketMaster(int seatCount) {
        reservedSeats = new RedBlackTree();
        // Define the priority queue, sorted by priority first, and by timestamp if priorities are equal
        waitlist = new PriorityQueue<>((user1, user2) -> {
            if (user1.getPriority() != user2.getPriority()) {
                return Integer.compare(user2.getPriority(), user1.getPriority()); // Higher priority first
            } else {
                return Long.compare(user1.getTimestamp(), user2.getTimestamp()); // Same priority, sort by timestamp
            }
        });
        availableSeats = new PriorityQueue<>();  // Min-heap for available seats
        userMap = new HashMap<>();   //User map to store users in the waitlist
        assignedSeats = new HashSet<>(); // Track assigned seats
        initializeSeats(seatCount); // Initialize seats
    }

    // Main program entry point
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Please provide an input file.");
            return;
        }

        String inputFileName = args[0];
        String outputFileName = inputFileName + "_output_file.txt";

        // Initialize the system
        GatorTicketMaster gtm = null;

        // Use try-with-resources to open input and output files
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[(), ]+");
                String command = parts[0];

                // execute corresponding operation
                switch (command) {
                    case "Initialize":   // Parse the seat count from the input, converting it to an integer
                        int seatCount = Integer.parseInt(parts[1]);
                        gtm = new GatorTicketMaster(seatCount);
                        writer.println("Initialized with " + seatCount + " seats.");
                        break;
                    case "Available":   // Check if the GatorTicketMaster system has been initialized
                        if (gtm != null) writer.println(gtm.available());
                        break;
                    case "Reserve":    // Parse the user ID and priority from the input, converting them to integers
                        int userID = Integer.parseInt(parts[1]);
                        int priority = Integer.parseInt(parts[2]);
                        if (gtm != null) writer.println(gtm.reserve(userID, priority));
                        break;
                    case "Cancel":
                        int seatID = Integer.parseInt(parts[1]);
                        userID = Integer.parseInt(parts[2]);
                        if (gtm != null) writer.println(gtm.cancel(seatID, userID));
                        break;
                    case "ExitWaitlist":
                        userID = Integer.parseInt(parts[1]);
                        if (gtm != null) writer.println(gtm.exitWaitlist(userID));
                        break;
                    case "UpdatePriority":
                        userID = Integer.parseInt(parts[1]);
                        priority = Integer.parseInt(parts[2]);
                        if (gtm != null) writer.println(gtm.updatePriority(userID, priority));
                        break;
                    case "AddSeats":   // Parse the number of new seats to add from the input
                        int count = Integer.parseInt(parts[1]);
                        if (gtm != null) {
                            gtm.addSeats(count, writer);
                        }
                        break;
                    case "PrintReservations":
                        if (gtm != null) writer.println(gtm.printReservations());
                        break;
                    case "ReleaseSeats":
                        int userID1 = Integer.parseInt(parts[1]);
                        int userID2 = Integer.parseInt(parts[2]);
                        if (gtm != null) writer.println(gtm.releaseSeats(userID1, userID2));
                        break;
                    case "Quit":
                        writer.println("Program Terminated!!");
                        return;
                    default:
                        writer.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Initialize seats by adding seat numbers to the available queue
    private void initializeSeats(int seatCount) {
        for (int i = 1; i <= seatCount; i++) {
            availableSeats.add(i);  //Add the seat  to the available seats.
        }
    }

    // Return information about currently available seats and the waitlist
    public String available() {
        int availableCount = availableSeats.size();
        return "Total Seats Available : " + availableCount + ", Waitlist : " + waitlist.size();
    }

    // Reserve a seat; if no seats are available, add to the waitlist
    public String reserve(int userID, int priority) {
        User user = new User(userID, priority);
        if (!availableSeats.isEmpty()) {
            int seatID = availableSeats.poll();  // get the lowest available seat
            assignedSeats.add(seatID); // mark seat as assigned
            reservedSeats.insert(userID, seatID); //reserve seat for user
            return "User " + userID + " reserved seat " + seatID;
        } else {
            long timestamp = System.nanoTime(); // Get current timestamp
            user.setTimestamp(timestamp); // Store timestamp in user object
            waitlist.offer(user); // Add user to waitlist
            userMap.put(userID, user); // Add user to user map
            return "User " + userID + " is added to the waiting list ";
        }
    }

    // Remove a user from the waitlist
    public String exitWaitlist(int userID) {
        if (userMap.containsKey(userID)) {   //  Check if the user is in the waitlist
            User user = userMap.get(userID);  // // Retrieve the user from the user map
            waitlist.remove(user);    // Remove the user from waitlist
            userMap.remove(userID);        // Remove the user from the user map
            return "User " + userID + " is removed from the waiting list";
        } else {
            return "User " + userID + " is not in waitlist";
        }
    }

    // Update user's priority
    public String updatePriority(int userID, int newPriority) {
        if (userMap.containsKey(userID)) {   //Check if the user exists  before update priority
            User user = userMap.get(userID);
            waitlist.remove(user);   // //Remove the user from the waitlist
            user.setPriority(newPriority); //update
            waitlist.offer(user);  //back to waitlist
            return "User " + userID + " priority has been updated to " + newPriority;
        } else {
            return "User " + userID + " priority is not updated";
        }
    }

    // Add new seats and try to allocate them to users in the waitlist
    public void addSeats(int count, PrintWriter writer) {    
     // Determine the starting seat number for new seats
    // If there are no assigned seats, start from seat number 1
    // Otherwise, start from the highest assigned seat number plus 1
        int startingSeat = assignedSeats.isEmpty() ? 1 : Collections.max(assignedSeats) + 1;
        for (int i = 0; i < count; i++) {
            int newSeat = startingSeat + i;
            availableSeats.add(newSeat);
            assignedSeats.add(newSeat);
        }

        // Write information about the newly added seats
        String message = "Additional " + count + " Seats are made available for reservation";
        writer.println(message);

        // Assign new seats to users in the waitlist
        while (!availableSeats.isEmpty() && !waitlist.isEmpty()) {
            User nextUser = waitlist.poll();   // get the highest-priority user from waitlist
            String reservationResult = reserve(nextUser.userID, nextUser.getPriority());
            writer.println(reservationResult);
        }
    }

    // Cancel a seat reservation; if users are on the waitlist, assign the seat to the next user
    public String cancel(int seatID, int userID) {
        if (reservedSeats.contains(userID) && reservedSeats.getSeatID(userID) == seatID) {
            reservedSeats.delete(userID);
            assignedSeats.remove(seatID); // Remove seat from assigned set
            availableSeats.offer(seatID); // Add canceled seat back to available queue

            if (!waitlist.isEmpty()) {
                User nextUser = waitlist.poll();   // Assign seat to next highest-priority user
                reserveSeatForUser(nextUser, seatID);
                assignedSeats.add(seatID); // Mark seat as assigned again
                availableSeats.remove(seatID); // Remove from available queue
                return "User " + userID + " canceled their reservation.\nUser " + nextUser.userID + " reserved seat " + seatID;
            }
            return "User " + userID + " canceled their reservation.";
        } else {
            return "User " + userID + " has no reservation for seat " + seatID + " to cancel.";
        }
    }


public String releaseSeats(int userID1, int userID2) {
    List<Integer> EmptySeats = new ArrayList<>();
    StringBuilder result = new StringBuilder();

    // 释放范围内用户的座位
    for (int userID = userID1; userID <= userID2; userID++) {
        if (reservedSeats.contains(userID)) {
            int seatID = reservedSeats.getSeatID(userID);
            reservedSeats.delete(userID);
            assignedSeats.remove(seatID);      // 从分配的座位集中移除该座位
            EmptySeats.add(seatID);            // 将座位添加到空闲座位列表中
        }
        if (userMap.containsKey(userID)) {
            exitWaitlist(userID);              // 如果用户在等待列表中，将其移除
        }
    }

    if (!EmptySeats.isEmpty()) {
        // 先添加释放信息
        result.append("Reservations of the Users in the range [")
                .append(userID1).append(", ").append(userID2).append("] are released\n");

        // 将释放的座位重新分配给等待列表中的用户
        result.append(assignEmptySeatsToWaitlist(EmptySeats)); // 获取分配信息并添加到结果

        // 如果有剩余未分配的空闲座位，将它们添加到可用座位列表中
        if (!EmptySeats.isEmpty()) {
            availableSeats.addAll(EmptySeats);
        }
    } else {
        result.append("No reservations found for the Users in the range [")
                .append(userID1).append(", ").append(userID2).append("].");
    }

    return result.toString().trim();
}
    private String assignEmptySeatsToWaitlist(List<Integer> seats) {
        Collections.sort(seats); // 排序座位，确保按编号从小到大分配
        StringBuilder allocationInfo = new StringBuilder();

        while (!seats.isEmpty() && !waitlist.isEmpty()) {
            int seatID = seats.remove(0);        // 获取并移除编号最小的座位
            User nextUser = waitlist.poll();     // 从等待列表中获取优先级最高的用户
            reserveSeatForUser(nextUser, seatID); // 为该用户分配座位

            // 添加分配信息到字符串中
            allocationInfo.append("User ").append(nextUser.userID)
                    .append(" reserved seat ").append(seatID).append("\n");
        }

        return allocationInfo.toString();
    }


    // Reserve a seat for a user
    private void reserveSeatForUser(User user, int seatID) {
        reservedSeats.insert(user.userID, seatID);    // Add seat to reserved seats
        assignedSeats.add(seatID); // Mark seat as assigned
        userMap.remove(user.userID);     // Remove user from waitlist
    }

    // Print all seat reservation information
    public String printReservations() {
        List<Map.Entry<Integer, Integer>> reservations = reservedSeats.getAllReservations();
        reservations.sort(Comparator.comparingInt(Map.Entry::getValue)); // Sort by seat number
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : reservations) {
            sb.append("Seat ").append(entry.getValue()).append(", User ").append(entry.getKey()).append("\n");
        }
        return sb.toString().trim();
    }

    // Red-Black Tree
    class RedBlackTree {
        private class Node {
            int userID;
            int seatID;
            Node left, right;
            boolean color;  //  node color (true for red, false for black)

            Node(int userID, int seatID) {
                this.userID = userID;
                this.seatID = seatID;
                this.color = true; // New nodes are red
            }
        }

        private Node root;  //root

        // Insert a new user's seat reservation information
        public void insert(int userID, int seatID) {
            root = insert(root, userID, seatID);
            root.color = false; // Root node is black
        }

        private Node insert(Node node, int userID, int seatID) {
            if (node == null) {       // if null, create a new node with the given user and seat ID
                return new Node(userID, seatID);
            }

            if (userID < node.userID) {      // Traverse the tree to find position
                node.left = insert(node.left, userID, seatID);  //insert in the left subtree
            } else if (userID > node.userID) {
                node.right = insert(node.right, userID, seatID);
            } else {
                node.seatID = seatID; // Update
            }

            return node;
        }

        // Check if a user's reservation information exists
        public boolean contains(int userID) {
            return getNode(root, userID) != null;
        }

        // Get the seat number for a specific user
        public int getSeatID(int userID) {
            Node node = getNode(root, userID);
            return (node != null) ? node.seatID : -1;
        }

        // Delete a user's reservation info
        public void delete(int userID) {
            if (contains(userID)) {
                root = deleteNode(root, userID);
            }
        }

        private Node getNode(Node node, int userID) {
            if (node == null) return null;
            if (userID < node.userID) return getNode(node.left, userID);
            else if (userID > node.userID) return getNode(node.right, userID);
            else return node;
        }

        private Node deleteNode(Node node, int userID) {  // Traverse the tree
            if (node == null) return null;
            if (userID < node.userID) node.left = deleteNode(node.left, userID);   //if less, go left
            else if (userID > node.userID) node.right = deleteNode(node.right, userID);
            else {
                if (node.left == null) return node.right;  // have no left child, return right child
                if (node.right == null) return node.left;  // have no right child, return left child
                Node minNode = getMin(node.right);   // have two children, go find minimum node in right subtree
                node.userID = minNode.userID;
                node.seatID = minNode.seatID;
                node.right = deleteNode(node.right, minNode.userID);
            }
            return node;
        }

        private Node getMin(Node node) {
            while (node.left != null) node = node.left;
            return node;
        }

        // Get all reservation information
        public List<Map.Entry<Integer, Integer>> getAllReservations() {
            List<Map.Entry<Integer, Integer>> reservations = new ArrayList<>();
            getAllReservations(root, reservations);
            return reservations;
        }

        private void getAllReservations(Node node, List<Map.Entry<Integer, Integer>> reservations) {
            if (node != null) {
                getAllReservations(node.left, reservations);
                reservations.add(new AbstractMap.SimpleEntry<>(node.userID, node.seatID));
                getAllReservations(node.right, reservations);
            }
        }
    }

    // User class containing user ID, priority, and timestamp
    class User {
        int userID;
        int priority;
        long timestamp;

        public User(int userID, int priority) {
            this.userID = userID;
            this.priority = priority;
            this.timestamp = System.nanoTime(); // Use nanosecond timestamp for uniqueness
        }

        public int getPriority() {
            return priority;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}