package org.mcreater.disk;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import com.google.gson.GsonBuilder;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.io.NBTInputStream;
import net.querz.nbt.tag.Tag;
import org.mcreater.disk.utils.TagUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

public class Main {
    PathTreeNode root;
    JTree tree;
    JFrame jf;
    Thread loadThread;
    JTextArea area;
    JLabel info;

    JProgressBar bar2;
    Thread dicLoadThread;
    File current;
    public static void main(String[] args) throws Throwable {
        new Main().doMain();
    }
    public static class PathTreeNode extends DefaultMutableTreeNode {
        public final File path;
        public PathTreeNode(String name, File path) {
            super(name);
            this.path = path;
        }
    }
    public void doMain(){
        FlatLightLaf.install();

        UIManager.installLookAndFeel("Flat Light 主题", "com.formdev.flatlaf.FlatLightLaf");
        UIManager.installLookAndFeel("Flat IntelliJ 主题", "com.formdev.flatlaf.FlatIntelliJLaf");
        UIManager.installLookAndFeel("Flat Dark 主题", "com.formdev.flatlaf.FlatDarkLaf");
        UIManager.installLookAndFeel("Flat Darcula 主题", "com.formdev.flatlaf.FlatDarculaLaf");
        UIManager.installLookAndFeel("Mac OS 主题", ch.randelshofer.quaqua.QuaquaManager.getLookAndFeelClassName());
        UIManager.installLookAndFeel("Acryl 主题", "com.jtattoo.plaf.acryl.AcrylLookAndFeel");
        UIManager.installLookAndFeel("Aero 主题", "com.jtattoo.plaf.aero.AeroLookAndFeel");
        UIManager.installLookAndFeel("Aluminium 主题", "com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
        UIManager.installLookAndFeel("Bernstein 主题", "com.jtattoo.plaf.bernstein.BernsteinLookAndFeel");
        UIManager.installLookAndFeel("Fast 主题", "com.jtattoo.plaf.fast.FastLookAndFeel");
        UIManager.installLookAndFeel("Graphite 主题", "com.jtattoo.plaf.graphite.GraphiteLookAndFeel");
        UIManager.installLookAndFeel("HiFi 主题", "com.jtattoo.plaf.hifi.HiFiLookAndFeel");
        UIManager.installLookAndFeel("Luna 主题", "com.jtattoo.plaf.luna.LunaLookAndFeel");
        UIManager.installLookAndFeel("Mc Win 主题", "com.jtattoo.plaf.mcwin.McWinLookAndFeel");
        UIManager.installLookAndFeel("Mint 主题", "com.jtattoo.plaf.mint.MintLookAndFeel");
        UIManager.installLookAndFeel("Noire 主题", "com.jtattoo.plaf.noire.NoireLookAndFeel");
        UIManager.installLookAndFeel("Smart 主题", "com.jtattoo.plaf.smart.SmartLookAndFeel");
        UIManager.installLookAndFeel("Texture 主题", "com.jtattoo.plaf.texture.TextureLookAndFeel");


        JFrame.setDefaultLookAndFeelDecorated(false);

        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());
        bar.add(createOpenMenu());
        bar.add(createThemeMenu());

        jf = new JFrame("文件打开器");
        jf.setSize(600, 400);
        jf.setJMenuBar(bar);

        info = new JLabel();
        bar2 = new JProgressBar(0, 100);

        String path = "D:/mods/util/.minecraft/versions/1.12.2/saves";

        root = new PathTreeNode(path, new File(path));

        area = new JTextArea();
        area.setEditable(false);

        tree = new JTree(new DefaultTreeModel(null));
        tree.addTreeSelectionListener(e -> {
            area.setText("");
            PathTreeNode dmt = (PathTreeNode) tree.getLastSelectedPathComponent();
            if (loadThread != null) loadThread.stop();
            loadThread = new Thread(() -> {
                if (dmt.path != null && dmt.path.isFile()) {
                    area.setText("");
                    jf.setTitle("文件打开器 - " + dmt.path.getName());
                    current = dmt.path;

                    try {
                        NBTInputStream stream = new NBTInputStream(new GZIPInputStream(Files.newInputStream(Paths.get(dmt.path.toURI()))));
                        Tag<?> tag = stream.readTag(Integer.MAX_VALUE).getTag();
                        area.append(new GsonBuilder().setPrettyPrinting().create().toJson(TagUtils.toNativeType(tag)));
                        return;
                    }
                    catch (Exception exc){}

                    try {
                        MCAFile file = MCAUtil.read(dmt.path);
                        Chunk chunk = file.getChunk(0, 0);
                        area.setText(new GsonBuilder().setPrettyPrinting().create().toJson(TagUtils.toNativeType(chunk.data)));
                        return;
                    }
                    catch (Exception exce) {}


                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(dmt.path));
                        Object[] raw = reader.lines().toArray();
                        long index = 0;
                        bar2.setMaximum(raw.length);
                        for (Object o : raw) {
                            area.append(o.toString() + "\n");
                            index++;
                            bar2.setString(index + " / " + raw.length);
                            bar2.setValue((int) index);
                        }
                        bar2.setString("");
                    } catch (Exception ex) {
                        bar2.setString("");
                        bar2.setValue(0);
                    }
                }
            });
            loadThread.start();
        });

        JScrollPane i1 = new JScrollPane(tree);
        JScrollPane i2 = new JScrollPane(area);

        new Thread(() -> {
            while (true) {
                int width = jf.getWidth();
                int height = jf.getHeight();

                i1.setBounds(0, 0, width / 3, height - 56 - 40);
                i2.setBounds(width / 3, 0, width / 3 * 2 - 15, height - 56 - 40);
                info.setBounds(0, height - 56 - 40, width / 3, 20);
                bar2.setBounds(width / 3, height - 90, width / 3 * 2 - 35, 20);
            }
        }).start();

        jf.add(i1);
        jf.add(i2);
        jf.add(info);
        jf.add(bar2);
        jf.resize(jf.getWidth(), jf.getHeight());

        GridLayout layout = new GridLayout();
        layout.setRows(1);
        layout.setColumns(3);

        jf.setLayout(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
        tree.setExpandsSelectedPaths(true);
    }
    public void addItems(PathTreeNode root, File path) {
        if (path.listFiles() != null) {
            CountDownLatch latch = new CountDownLatch(path.listFiles().length);
            AtomicLong lon = new AtomicLong(path.listFiles().length);
            for (File item : path.listFiles()) {
                PathTreeNode item2 = new PathTreeNode(item.getName(), item);
                info.setText("   " + item.getName());
                root.add(item2);
                if (item.isDirectory()) {
                    if (item.listFiles() != null){
                        addItems(item2, item);
                        if (item.listFiles().length == 0) {
                            item2.add(new PathTreeNode("<empty dictionary>", new File("")));
                        }
                    }
                }
            }
        }
    }
    private JMenu createOpenMenu() {
        JMenu menu=new JMenu("运行(R)");
        menu.setMnemonic(KeyEvent.VK_R);    //设置快速访问符
        JMenuItem item = new JMenuItem("");
        item.setAction(new AbstractAction("用关联的程序打开(T)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (current != null) {
                        try {
                            desktop.open(current);
                        } catch (IOException ex) {

                        }
                    }
                }
            }
        });
        menu.add(item);

        return menu;
    }
    private JMenu createThemeMenu() {
        JMenu menu=new JMenu("主题(T)");
        menu.setMnemonic(KeyEvent.VK_T);    //设置快速访问符
        JMenuItem item;
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            item = new JMenuItem("");
            item.setAction(new AbstractAction(info.getName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setLookAndFeel(info.getClassName());
                }
            });
            menu.add(item);
        }

        return menu;
    }

    private void setLookAndFeel(String feel){
        try {
            UIManager.setLookAndFeel(feel);
            SwingUtilities.updateComponentTreeUI(jf);
            JFrame.setDefaultLookAndFeelDecorated(false);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private JMenu createFileMenu() {
        JMenu menu=new JMenu("文件(F)");
        menu.setMnemonic(KeyEvent.VK_F);    //设置快速访问符
        JMenuItem item=new JMenuItem("",KeyEvent.VK_O);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        item.setAction(new AbstractAction("打开(O)") {
            public void actionPerformed(ActionEvent e) {
                if (loadThread != null) loadThread.stop();
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int result = chooser.showOpenDialog(jf);
                if (result == JFileChooser.APPROVE_OPTION) {
                    area.setText("");
                    String path = chooser.getSelectedFile().getAbsolutePath();
                    root = new PathTreeNode(path, new File(path));

                    if (dicLoadThread != null) dicLoadThread.stop();
                    dicLoadThread = new Thread(() -> {
                        tree.setEnabled(false);
                        addItems(root, new File(path));
                        tree.setModel(new DefaultTreeModel(root, false));
                        tree.setEnabled(true);
                        info.setText("");
                    });
                    dicLoadThread.start();
                }
            }
        });
        menu.add(item);
        menu.addSeparator();
        item=new JMenuItem("",KeyEvent.VK_E);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        item.setAction(new AbstractAction("退出(E)") {
            public void actionPerformed(ActionEvent e) {
                jf.hide();
                System.exit(0);
            }
        });
        menu.add(item);
        return menu;
    }
}