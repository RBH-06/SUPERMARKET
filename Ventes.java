package com.mycompany.projetgl.presentation;
import com.mycompany.projetgl.DAO.DBConnection;
import com.mycompany.projetgl.ProduitDAO;
import com.mycompany.projetgl.metier.GestionVentes;
import com.mycompany.projetgl.LigneVente;
import com.mycompany.projetgl.Produit;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 *
 * @author InfoPro
 */
public class Ventes extends javax.swing.JFrame {
private ProduitDAO produitDAO;
    private GestionVentes gestionVentes;
    private List<Produit> listeProduitsStock; 
    private List<LigneVente> panier = new ArrayList<>(); 
    private double totalVente = 0.0;

    // --- STYLE (COPIÉ DE STOCK.JAVA) ---
    private final Color MAIN_BG_COLOR = new Color(240, 242, 245);
    private final Color PRIMARY_TEXT = new Color(44, 62, 80);
    private final Color VALIDATE_COLOR = new Color(46, 204, 113); // Vert
    private final Color DELETE_COLOR = new Color(231, 76, 60);    // Rouge
    private final Color TABLE_HEADER_BG = new Color(248, 249, 250);
    
    private final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font TOTAL_FONT = new Font("Segoe UI", Font.BOLD, 36); // Très gros pour le total
    public Ventes() {
        initComponents();
   initCustom();
    }
  private void initCustom() {
        this.getContentPane().setBackground(MAIN_BG_COLOR);
        this.setLocationRelativeTo(null);
        
        // Connexion BDD
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            produitDAO = new ProduitDAO(conn);
            gestionVentes = new GestionVentes(conn);
        } else {
            JOptionPane.showMessageDialog(this, "Erreur Connexion BDD !", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        
        // --- CORRECTION ICI : ON REND LES TABLEAUX NON ÉDITABLES ---
        
        // 1. Tableau Stock (Gauche)
        jTable1.setModel(new DefaultTableModel(
            new Object[][]{}, 
            new String[]{"ID", "Nom", "Prix", "Stock"}
        ) {
            // Cette méthode empêche l'édition des cellules
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        });

        // 2. Tableau Panier (Droite)
        jTable2.setModel(new DefaultTableModel(
            new Object[][]{}, 
            new String[]{"Produit", "Prix U.", "Qté", "Total"}
        ) {
            // Idem pour le panier
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        });
        
        // Suite de l'initialisation...
        initStyles();
        initEventsComplementaires();
        chargerStock();
    }

    // --- STYLE ---
    private void initStyles() {
        styleTable(jTable1);
        styleTable(jTable2);
        styleButton(jButton1, VALIDATE_COLOR);
        styleButton(jButton2, DELETE_COLOR);
        
        jLabel1.setFont(TOTAL_FONT);
        jLabel1.setForeground(PRIMARY_TEXT);
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    // --- RECHERCHE ET DOUBLE CLIC ---
    private void initEventsComplementaires() {
        jTextField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { filtrerStock(); }
        });

        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) ajouterAuPanier();
            }
        });
    }

    // =========================================================================
    // LOGIQUE MÉTIER
    // =========================================================================

    private void chargerStock() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (produitDAO != null) {
            listeProduitsStock = produitDAO.getProduits(); 
            for (Produit p : listeProduitsStock) {
                if (p.getQteStock() > 0) {
                    model.addRow(new Object[]{ p.getId(), p.getNom(), p.getPrix() + " €", p.getQteStock() });
                }
            }
        }
    }

    private void filtrerStock() {
        String query = jTextField1.getText().toLowerCase();
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (listeProduitsStock != null) {
            for (Produit p : listeProduitsStock) {
                if ((p.getNom().toLowerCase().contains(query) || p.getCodeBarre().contains(query)) && p.getQteStock() > 0) {
                    model.addRow(new Object[]{ p.getId(), p.getNom(), p.getPrix() + " €", p.getQteStock() });
                }
            }
        }
    }

  private void ajouterAuPanier() {
        int row = jTable1.getSelectedRow();
        if (row == -1) return;

        try {
            int idProd = Integer.parseInt(jTable1.getValueAt(row, 0).toString());
            String nomProd = jTable1.getValueAt(row, 1).toString();
            double prixProd = Double.parseDouble(jTable1.getValueAt(row, 2).toString().replace(" €", "").replace(",", "."));
            int stockMax = Integer.parseInt(jTable1.getValueAt(row, 3).toString());

            String qteStr = JOptionPane.showInputDialog(this, "Quantité pour " + nomProd + " ?", "1");
            if (qteStr == null) return;

            int qte = Integer.parseInt(qteStr);
            if (qte <= 0 || qte > stockMax) {
                JOptionPane.showMessageDialog(this, "Quantité invalide ou stock insuffisant !");
                return;
            }

            LigneVente ligne = new LigneVente(idProd, nomProd, qte, prixProd);
            panier.add(ligne);
            
            DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
            model.addRow(new Object[]{ nomProd, String.format("%.2f €", prixProd), qte, String.format("%.2f €", (qte * prixProd)) });

            totalVente += (qte * prixProd);
            jLabel1.setText(String.format("%.2f €", totalVente));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
        }
    }
    
    // Méthode utilitaire pour nettoyer (appelée par les deux boutons)
    private void resetInterface() {
        panier.clear();
        ((DefaultTableModel) jTable2.getModel()).setRowCount(0);
        totalVente = 0.0;
        jLabel1.setText("0.00 €");
        jTextField2.setText("");
        jTextField1.setText(""); 
        chargerStock(); 
    }

    // --- OUTILS STYLE ---
    private void styleButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setRowHeight(45);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(PRIMARY_TEXT);
        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(new Color(100, 100, 100));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 220, 220)));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    
     
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nom", "Stock", "Prix"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nom", "Qté", "Prix-Unitaire", "Total"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("0.00€");

        jButton1.setText("Valider vente");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Annuler");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Montant recu :");

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel3.setText("Rechercher :");

        jPanel1.setBackground(new java.awt.Color(44, 62, 80));

        jButton3.setBackground(new java.awt.Color(44, 62, 80));
        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton3.setForeground(new java.awt.Color(255, 255, 255));
        jButton3.setText("CAISSE");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setBackground(new java.awt.Color(44, 62, 80));
        jButton4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton4.setForeground(new java.awt.Color(255, 255, 255));
        jButton4.setText("ADMINISTRATION");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setBackground(new java.awt.Color(44, 62, 80));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("STOCK");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(132, 132, 132)
                .addComponent(jButton4)
                .addGap(107, 107, 107)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 137, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addGap(136, 136, 136))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(38, 38, 38)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(52, 52, 52))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(162, 162, 162))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(37, 37, 37)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(115, 115, 115))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(89, 89, 89)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 425, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextField1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {                                            
        // TODO add your handling code here:
    }                                           

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
       if (panier.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Impossible : Le panier est vide !");
            return;
        }
        try {
            // Vérification du montant reçu (Si rempli)
            if (!jTextField2.getText().isEmpty()) {
                double recu = Double.parseDouble(jTextField2.getText().replace(",", "."));
                if (recu < totalVente) {
                    JOptionPane.showMessageDialog(this, "Erreur : Montant reçu insuffisant !");
                    return;
                }
                JOptionPane.showMessageDialog(this, "Monnaie à rendre : " + String.format("%.2f €", (recu - totalVente)));
            }

            // Enregistrement en base de données
            gestionVentes.enregistrerVente(panier, totalVente, 1);
            
            JOptionPane.showMessageDialog(this, "Vente validée avec succès !");
            
            // Nettoyage après succès
            resetInterface();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur critique : " + e.getMessage());
            e.printStackTrace();
        }
    }                                        

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
      int rep = JOptionPane.showConfirmDialog(this, "Voulez-vous annuler et vider le panier ?", "Annulation", JOptionPane.YES_NO_OPTION);
        if (rep == JOptionPane.YES_OPTION) {
            resetInterface();
        }
    }                                        

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {                                            
       
    }                                           

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        new AdministrationGlobal().setVisible(true);
    this.dispose();
    }                                        

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {                                         
     new Ventes().setVisible(true);
    
    // 2. Fermer la fenêtre actuelle
    this.dispose();
    }                                        

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {                                         
       new Stock().setVisible(true);
    this.dispose();
    }                                        

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
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
            java.util.logging.Logger.getLogger(Ventes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Ventes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Ventes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Ventes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Ventes().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration                   
}
