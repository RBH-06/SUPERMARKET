package com.mycompany.projetgl.presentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;


import com.mycompany.projetgl.DAO.DBConnection;
import com.mycompany.projetgl.Produit;
import com.mycompany.projetgl.ProduitDAO;
import com.mycompany.projetgl.metier.GestionProduit;
import java.sql.*;

/**
 *
 * @author InfoPro
 */
// --- 1. CONFIGURATION DU STYLE ---
 
public class Stock extends javax.swing.JFrame {
// --- 1. CONFIGURATION DU STYLE ---
    private GestionProduit serviceProduit;
    
  private final Color MAIN_BG_COLOR = new Color(240, 242, 245);
    private final Color PRIMARY_TEXT = new Color(44, 62, 80);
    private final Color BUTTON_COLOR = new Color(52, 100, 219);
    private final Color ALERT_COLOR = new Color(231, 76, 60);
    private final Color VALIDATE_COLOR = new Color(46, 204, 113); // Vert 
    private final Color DELETE_COLOR = new Color(231, 76, 60); 
    private final Color TABLE_HEADER_BG = new Color(248, 249, 250);
    
    private final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    
    public Stock() {
        initComponents();
        initCustom();
       
    }
private void initCustom() {
        getContentPane().setBackground(MAIN_BG_COLOR);
        setLocationRelativeTo(null);
        
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            serviceProduit = new GestionProduit(new ProduitDAO(conn));
        } else {
            JOptionPane.showMessageDialog(this, "Erreur BDD !", "Erreur", JOptionPane.ERROR_MESSAGE);
        }

        styleTable(jTable1);
        styleButton(jButton1); 
        activerActions();
        chargerDonnees();
    }

    // --- CHARGEMENT SIMPLIFIÉ (Plus de boucle ID->Nom) ---
    private void chargerDonnees() {
        if (serviceProduit == null) return;
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        
        for (Produit p : serviceProduit.listerProduits()) {
            model.addRow(new Object[]{
                p.getId(), 
                p.getCodeBarre(), 
                p.getNom(), 
                p.getCategorie(), // <--- On affiche directement le texte !
                p.getPrix() + " €", 
                p.getQteStock(), 
                ""
            });
        }
    }
   private void actionModifier(int row) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        
        String nom = (String) model.getValueAt(row, 2);
        String cat = (String) model.getValueAt(row, 3);
        String prixS = model.getValueAt(row, 4).toString().replace(" €", "");
        String stockS = model.getValueAt(row, 5).toString();
        
        JTextField tNom = new JTextField(nom);
        JTextField tCat = new JTextField(cat);
        JTextField tPrix = new JTextField(prixS);
        JTextField tStock = new JTextField(stockS);

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Nom :"));       p.add(tNom);
        p.add(new JLabel("Catégorie :")); p.add(tCat);
        p.add(new JLabel("Prix :"));      p.add(tPrix);
        p.add(new JLabel("Stock :"));     p.add(tStock);

        if (JOptionPane.showConfirmDialog(this, p, "Modifier", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                Produit prod = serviceProduit.getProduitById(id);
                if (prod != null) {
                    prod.setNom(tNom.getText());
                    prod.setCategorie(tCat.getText()); // Modif directe texte
                    prod.setPrix(Double.parseDouble(tPrix.getText().replace(",", ".")));
                    prod.setQteStock(Integer.parseInt(tStock.getText()));
                    
                    serviceProduit.mettreAJourProduit(prod);
                    chargerDonnees();
                    JOptionPane.showMessageDialog(this, "Modifié !");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
            }
        }
    }
      

    private void activerActions() {
        jTable1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int col = jTable1.columnAtPoint(e.getPoint());
                int row = jTable1.rowAtPoint(e.getPoint());
                if (col == 6 && row >= 0) {
                    if (e.getX() - jTable1.getCellRect(row, col, false).x < jTable1.getColumnModel().getColumn(6).getWidth() / 2) 
                        actionModifier(row);
                    else if (JOptionPane.showConfirmDialog(null, "Supprimer ?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        try {
                            serviceProduit.supprimerProduit(Integer.parseInt(jTable1.getValueAt(row, 0).toString()));
                            chargerDonnees();
                        } catch(Exception ex) {}
                    }
                }
            }
        });
    }

    private void styleButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(BUTTON_COLOR);
        b.setForeground(Color.WHITE);
    }

    private void styleTable(JTable t) {
        t.setRowHeight(45);
        t.setFont(TABLE_FONT);
        t.getTableHeader().setBackground(new Color(248, 249, 250));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<t.getColumnCount()-1; i++) t.getColumnModel().getColumn(i).setCellRenderer(center);

        // Stock Rouge
        try {
            t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(JLabel.CENTER);
                    try { if (Integer.parseInt(value.toString()) < 5) { setForeground(ALERT_COLOR); setFont(TABLE_FONT.deriveFont(Font.BOLD)); } else setForeground(Color.BLACK); } catch(Exception e){}
                    return this;
                }
            });
        } catch(Exception e){}

        // Boutons
        t.getColumnModel().getColumn(6).setMinWidth(200);
        t.getColumnModel().getColumn(6).setCellRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new GridLayout(1, 2, 5, 0));
                p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                JButton b1 = new JButton("Modif"); b1.setBackground(VALIDATE_COLOR); b1.setForeground(Color.WHITE);
                JButton b2 = new JButton("Suppr"); b2.setBackground(DELETE_COLOR); b2.setForeground(Color.WHITE);
                p.add(b1); p.add(b2);
                return p;
            }
        });
    }
      
    
    

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {}
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Stock().setVisible(true);
            }
        });
    }
    

        
      

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setForeground(new java.awt.Color(204, 204, 204));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Gestion de stocks");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "code_barre", "Nom", "Catégorie", "Prix", "Stock", "Action"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jButton1.setText("Nouveau Produit");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(254, 254, 254)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(48, 48, 48))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 991, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel2.setBackground(new java.awt.Color(44, 62, 80));

        jButton2.setBackground(new java.awt.Color(44, 62, 80));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setText("STOCK");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setBackground(new java.awt.Color(44, 62, 80));
        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton3.setForeground(new java.awt.Color(255, 255, 255));
        jButton3.setText("ADMINISTRATION");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("DECONNEXION");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setBackground(new java.awt.Color(44, 62, 80));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("CAISSE");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addGap(30, 30, 30))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(52, 52, 52)
                .addComponent(jButton4)
                .addGap(86, 86, 86)
                .addComponent(jButton3)
                .addGap(118, 118, 118)
                .addComponent(jButton5)
                .addGap(107, 107, 107)
                .addComponent(jButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>                        

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
   JTextField tCode = new JTextField();
        JTextField tNom = new JTextField();
        JTextField tCat = new JTextField(); 
        JTextField tPrix = new JTextField();
        JTextField tStock = new JTextField();

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Code Barre :")); p.add(tCode);
        p.add(new JLabel("Nom :"));        p.add(tNom);
        p.add(new JLabel("Catégorie :"));  p.add(tCat);
        p.add(new JLabel("Prix :"));       p.add(tPrix);
        p.add(new JLabel("Stock :"));      p.add(tStock);

        if (JOptionPane.showConfirmDialog(this, p, "Nouveau", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String code = tCode.getText().trim().isEmpty() ? "REF-" + System.currentTimeMillis() : tCode.getText().trim();
                double prix = Double.parseDouble(tPrix.getText().replace(",", "."));
                int stock = Integer.parseInt(tStock.getText());

                if (serviceProduit != null) {
                    // On envoie directement le texte de la catégorie
                    serviceProduit.ajouterProduit(tNom.getText(), prix, code, stock, 5, tCat.getText());
                    chargerDonnees();
                    JOptionPane.showMessageDialog(this, "Produit ajouté !");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
            }
        }
    }                                        

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        new Stock().setVisible(true);
    this.dispose();
    }                                        

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {                                         
  new Ventes().setVisible(true);
    
    // 2. Fermer la fenêtre actuelle
    this.dispose();
    }                                        

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        // TODO add your handling code here:
    }                                        

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {                                         
     new AdministrationGlobal().setVisible(true);
    this.dispose();
    }                                        

    

  

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration                   
}


