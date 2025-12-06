package com.mycompany.projetgl.metier;
import com.mycompany.projetgl.LigneVente;
import com.mycompany.projetgl.LigneVenteDAO;
import com.mycompany.projetgl.Produit;
import com.mycompany.projetgl.ProduitDAO;
import com.mycompany.projetgl.Vente;
import com.mycompany.projetgl.VenteDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public class GestionVentes {

    private Connection connection;
    private VenteDAO venteDAO;
    private LigneVenteDAO ligneVenteDAO;
    private ProduitDAO produitDAO;

    // Constructeur
    public GestionVentes(Connection connection) {
        this.connection = connection;
        this.venteDAO = new VenteDAO(connection);
        this.ligneVenteDAO = new LigneVenteDAO(connection);
        this.produitDAO = new ProduitDAO(connection);
    }

    // --- MÉTHODE TRANSACTIONNELLE ---
    public void enregistrerVente(List<LigneVente> panier, double total, int employeId) throws Exception {
        
        try {
            // 1. DÉMARRAGE DE LA TRANSACTION
            // On bloque l'enregistrement automatique pour s'assurer que TOUT passe ou RIEN ne passe
            connection.setAutoCommit(false); 

            // 2. CRÉATION DU TICKET DE VENTE
            Vente vente = new Vente(total, employeId);
            int venteId = venteDAO.creerVente(vente);
            
            if (venteId == -1) {
                throw new Exception("Erreur critique : Impossible de créer le ticket de vente.");
            }

            // 3. TRAITEMENT DE CHAQUE LIGNE (ARTICLE)
            for (LigneVente ligne : panier) {
                // A. On lie la ligne au ticket qu'on vient de créer
                ligne.setVenteId(venteId);
                
                // B. On sauvegarde la ligne dans la table lignes_ventes
                ligneVenteDAO.ajouterLigne(ligne);
                
                // C. GESTION DU STOCK (Mise à jour)
                Produit p = produitDAO.getProduitById(ligne.getProduitId());
                
                if (p != null) {
                    // Calcul du nouveau stock
                    int nouveauStock = p.getQteStock() - ligne.getQuantite();
                    
                    // Vérification : on ne peut pas vendre ce qu'on n'a pas
                    if (nouveauStock < 0) {
                        throw new Exception("Stock insuffisant pour le produit : " + p.getNom());
                    }
                    
                    // Mise à jour de l'objet
                    p.setQteStock(nouveauStock);
                    
                    // --- CHANGEMENT ICI ---
                    // On appelle la méthode simplifiée (sans ID fournisseur ni ID catégorie)
                    // Le DAO s'occupe de tout car l'objet 'p' contient déjà toutes les infos
                    produitDAO.mettreAJourProduit(p);
                }
            }

            // 4. VALIDATION (COMMIT)
            // Tout s'est bien passé, on valide les changements dans la BDD
            connection.commit(); 
            System.out.println("Vente enregistrée avec succès !");

        } catch (Exception e) {
            // 5. ANNULATION (ROLLBACK)
            // En cas d'erreur (ex: stock insuffisant), on annule tout ce qu'on vient de faire
            try { 
                if (connection != null) connection.rollback(); 
                System.out.println("Annulation de la vente.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            // On renvoie l'erreur à l'interface pour afficher un message à l'utilisateur
            throw new Exception("Échec de la vente : " + e.getMessage());
            
        } finally {
            // On remet la connexion en mode normal
            try { 
                if (connection != null) connection.setAutoCommit(true); 
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
