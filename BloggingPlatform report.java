//Blogging Platform with Role-Based Access - Integrated Version

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


//================================================================
// 1. DATA MODELS & ENTITIES
//================================================================

/**
 * Abstract base class for all user types. Defines common properties
 * and the abstract dashboard method for role-specific implementations.
 */
abstract class User {
    protected String username;
    protected String password;
    protected String role;

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public abstract void showDashboard(BloggingPlatform platform, JFrame parentFrame);
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public boolean checkPassword(String password) { return this.password.equals(password); }
}

class Admin extends User {
    public Admin(String username, String password) {
        super(username, password, "Admin");
    }

    @Override
    public void showDashboard(BloggingPlatform platform, JFrame parentFrame) {
        JOptionPane.showMessageDialog(parentFrame, "Welcome, Admin! You can manage users and posts.", "Admin Dashboard", JOptionPane.INFORMATION_MESSAGE);
        // In a real app, this would open a new management window.
    }
}

class Author extends User {
    public Author(String username, String password) {
        super(username, password, "Author");
    }
    
    @Override
    public void showDashboard(BloggingPlatform platform, JFrame parentFrame) {
        BlogGUI.createAuthorDashboard(platform, parentFrame);
    }
}

class Reader extends User {
    public Reader(String username, String password) {
        super(username, password, "Reader");
    }

    @Override
    public void showDashboard(BloggingPlatform platform, JFrame parentFrame) {
        BlogGUI.createReaderDashboard(platform, parentFrame);
    }
}

/**
 * Represents a single blog post. Now includes its own CommentSystem.
 */
class Post {
    private static int counter = 1;
    private int id;
    private String title;
    private String content;
    private String author;
    private CommentSystem commentSystem = new CommentSystem();

    public Post(String title, String content, String author) {
        this.id = counter++;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public CommentSystem getCommentSystem() { return commentSystem; }

    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- POST [" + id + "] ---\n");
        sb.append("Title: " + title + "\n");
        sb.append("By: " + author + "\n");
        sb.append("Content: " + content + "\n");
        sb.append("Comments:\n");
        sb.append(commentSystem.getCommentsDisplayText());
        sb.append("---------------------\n");
        return sb.toString();
    }
}

class PostNotFoundException extends Exception {
    public PostNotFoundException(String msg) { super(msg); }
}

/**
 * Generic Comment class.
 */
class Comment<T> {
    private T text;
    private String byUser;

    public Comment(T text, String byUser) {
        this.text = text;
        this.byUser = byUser;
    }

    public T getText() { return text; }
    public String getByUser() { return byUser; }
}

//================================================================
// 2. SYSTEM & LOGIC COMPONENTS
//================================================================

/**
 * Manages a list of comments for a single post.
 */
class CommentSystem {
    private List<Comment<String>> comments = new ArrayList<>();

    public synchronized void addComment(Comment<String> c) {
        comments.add(c);
        System.out.println("Comment added by " + c.getByUser() + ": " + c.getText());
    }
    
    public String getCommentsDisplayText() {
        if (comments.isEmpty()) {
            return "  No comments yet.\n";
        }
        return comments.stream()
                .map(c -> "  - " + c.getText() + " (" + c.getByUser() + ")")
                .collect(Collectors.joining("\n")) + "\n";
    }
}


/**
 * Runnable task to notify an author of a new comment.
 */
class CommentNotifier implements Runnable {
    private String authorName;
    public CommentNotifier(String authorName) { this.authorName = authorName; }

    @Override
    public void run() {
        System.out.println("Notifier: Preparing to send notification to " + authorName);
        try {
            // Simulate network latency or processing time
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("✅ Notifier: New comment notification sent to " + authorName);
    }
}

/**
 * Background server thread.
 */
class BlogServer {
    public static void startServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(5000)) {
                System.out.println("🌐 Server started on port 5000...");
                while (true) {
                    Socket client = server.accept();
                    System.out.println("Client connected: " + client.getInetAddress());
                    // In a real app, you'd handle the client connection in a new thread.
                    client.close();
                }
            } catch (IOException e) {
                // e.printStackTrace();
                System.err.println("Could not start server on port 5000. Is it already in use?");
            }
        }).start();
    }
}

/**
 * Mock Database Manager.
 */
class DatabaseManager {
    public static void savePost(Post post) {
        System.out.println("💾 Pretend saving post '" + post.getTitle() + "' to DB...");
    }
}


//================================================================
// 3. GRAPHICAL USER INTERFACE
//================================================================

class BlogGUI {
    
    /**
     * Creates and displays the main login window.
     * @param platform The central blogging platform instance.
     */
    public static void createLoginGUI(BloggingPlatform platform) {
        JFrame frame = new JFrame("Blogging Platform Login");
        frame.setSize(350, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the window

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(10);
        
        // Pre-fill for easier testing
        userField.setText("author1");

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(10);
        passField.setText("abcd"); // Pre-fill

        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit");
        
        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            if (platform.login(username, password)) {
                frame.dispose(); // Close login window
                platform.getCurrentUser().showDashboard(platform, frame); // Show appropriate dashboard
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        exitButton.addActionListener(e -> platform.shutdown());

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(loginButton);
        panel.add(exitButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    /**
     * Displays the dashboard for an Author user.
     */
    public static void createAuthorDashboard(BloggingPlatform platform, JFrame parent) {
        JFrame frame = new JFrame("Author Dashboard - " + platform.getCurrentUser().getUsername());
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(parent);
        
        JTextArea postsArea = new JTextArea(platform.getAllPostsAsText());
        postsArea.setEditable(false);
        
        JButton createPostButton = new JButton("Create New Post");
        createPostButton.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(frame, "Enter post title:");
            if (title != null && !title.trim().isEmpty()) {
                 String content = JOptionPane.showInputDialog(frame, "Enter post content:");
                 if (content != null && !content.trim().isEmpty()) {
                     platform.createPost(title, content);
                     postsArea.setText(platform.getAllPostsAsText()); // Refresh view
                 }
            }
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createPostButton);

        frame.add(new JScrollPane(postsArea), BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                platform.shutdown();
            }
        });
    }

    /**
     * Displays the dashboard for a Reader user.
     */
    public static void createReaderDashboard(BloggingPlatform platform, JFrame parent) {
        JFrame frame = new JFrame("Reader Dashboard - " + platform.getCurrentUser().getUsername());
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(parent);

        JTextArea postsArea = new JTextArea(platform.getAllPostsAsText());
        postsArea.setEditable(false);
        
        JButton addCommentButton = new JButton("Add Comment");
        addCommentButton.addActionListener(e -> {
            try {
                String postIdStr = JOptionPane.showInputDialog(frame, "Enter ID of post to comment on:");
                int postId = Integer.parseInt(postIdStr);
                String commentText = JOptionPane.showInputDialog(frame, "Enter your comment:");
                if (commentText != null && !commentText.trim().isEmpty()) {
                    platform.addComment(postId, commentText);
                    postsArea.setText(platform.getAllPostsAsText()); // Refresh view
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid Post ID.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (PostNotFoundException ex) {
                 JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addCommentButton);

        frame.add(new JScrollPane(postsArea), BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                platform.shutdown();
            }
        });
    }
}


//================================================================
// 4. MAIN APPLICATION CLASS
//================================================================

public class BloggingPlatform {
    private final Map<String, User> users = new HashMap<>();
    private final List<Post> posts = new ArrayList<>();
    private User currentUser;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * Constructor initializes the system with sample data.
     */
    public BloggingPlatform() {
        // Initialize with some default users
        users.put("admin1", new Admin("admin1", "1234"));
        User author1 = new Author("author1", "abcd");
        users.put("author1", author1);
        users.put("reader1", new Reader("reader1", "xyz"));
        
        // Initialize with a default post
        Post initialPost = new Post("Welcome to the Blog!", "This is the very first post.", author1.getUsername());
        posts.add(initialPost);
    }
    
    /**
     * Attempts to log a user in.
     * @return true if login is successful, false otherwise.
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            this.currentUser = user;
            System.out.println("Login successful for user: " + username + " (" + user.getRole() + ")");
            return true;
        }
        System.out.println("Login failed for user: " + username);
        return false;
    }
    
    /**
     * Creates a new post, requires the current user to be an Author.
     */
    public void createPost(String title, String content) {
        if (currentUser instanceof Author) {
            Post newPost = new Post(title, content, currentUser.getUsername());
            posts.add(newPost);
            DatabaseManager.savePost(newPost);
            System.out.println("Post created: " + title);
        } else {
            System.out.println("Only Authors can create posts.");
        }
    }
    
    /**
     * Adds a comment to a specific post.
     */
    public void addComment(int postId, String commentText) throws PostNotFoundException {
        Optional<Post> postOpt = posts.stream().filter(p -> p.getId() == postId).findFirst();
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            Comment<String> comment = new Comment<>(commentText, currentUser.getUsername());
            post.getCommentSystem().addComment(comment);
            
            // Notify the post's author of the new comment
            executor.execute(new CommentNotifier(post.getAuthor()));
        } else {
            throw new PostNotFoundException("Post with ID " + postId + " not found.");
        }
    }

    public User getCurrentUser() { return currentUser; }
    
    public String getAllPostsAsText() {
        if (posts.isEmpty()) {
            return "No posts available.";
        }
        return posts.stream()
                .map(Post::getDisplayText)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Shuts down the executor service and exits the application.
     */
    public void shutdown() {
        System.out.println("Shutting down the application...");
        executor.shutdown();
        System.exit(0);
    }

    /**
     * Main entry point of the application.
     */
    public static void main(String[] args) {
        // Start the background server
        BlogServer.startServer();

        // Create the main application logic controller
        BloggingPlatform platform = new BloggingPlatform();
        
        // Schedule the GUI to be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> BlogGUI.createLoginGUI(platform));
    }
}