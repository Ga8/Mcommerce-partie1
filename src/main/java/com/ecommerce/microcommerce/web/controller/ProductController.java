package com.ecommerce.microcommerce.web.controller;

import com.ecommerce.microcommerce.dao.ProductDao;
import com.ecommerce.microcommerce.model.Product;
import com.ecommerce.microcommerce.web.exceptions.ProduitGratuitException;
import com.ecommerce.microcommerce.web.exceptions.ProduitIntrouvableException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


@Api( description="API pour es opérations CRUD sur les produits.")

@RestController
public class ProductController {

    @Autowired
    private ProductDao productDao;



    @RequestMapping(value = "/Produits", method = RequestMethod.GET)
    public MappingJacksonValue listeProduits() {

        Iterable<Product> produits = productDao.findAll();

        SimpleBeanPropertyFilter monFiltre = SimpleBeanPropertyFilter.serializeAllExcept("prixAchat");

        FilterProvider listDeNosFiltres = new SimpleFilterProvider().addFilter("monFiltreDynamique", monFiltre);

        MappingJacksonValue produitsFiltres = new MappingJacksonValue(produits);

        produitsFiltres.setFilters(listDeNosFiltres);

        return produitsFiltres;
    }


    //Récupérer un produit par son Id
    @ApiOperation(value = "Récupère un produit grâce à son ID à condition que celui-ci soit en stock!")
    @GetMapping(value = "/Produits/{id}")
    public Product afficherUnProduit(@PathVariable int id) {

        Product produit = productDao.findById(id);

        if(produit==null) throw new ProduitIntrouvableException("Le produit avec l'id " + id + " est INTROUVABLE. Écran Bleu si je pouvais.");

        return produit;
    }




    //ajouter un produit
    @PostMapping(value = "/Produits")
    public ResponseEntity<Void> ajouterProduit(@Valid @RequestBody Product product) {

        Product productAdded =  productDao.save(product);

        if (productAdded == null)
            return ResponseEntity.noContent().build();

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productAdded.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @DeleteMapping (value = "/Produits/{id}")
    public void supprimerProduit(@PathVariable int id) {

        productDao.delete(id);
    }

    @PutMapping (value = "/Produits")
    public void updateProduit(@RequestBody Product product) {

        productDao.save(product);
    }


    //Pour les tests
    @GetMapping(value = "test/produits/{prix}")
    public List<Product>  testeDeRequetes(@PathVariable int prix) {

        return productDao.chercherUnProduitCher(400);
    }

    @ApiOperation(value = "Retourne tous les produits de la base et calcule la marge associé au produit.")
    @GetMapping(value = "/AdminProduits")
    public List<String> calculerMargeProduit(){
        List<String> productWithBenefit= new ArrayList<>();

        //récupération des données en base comme demandé
        List<Product> products =  productDao.findAll();

        checkPrixNull(products);
        for (Product product :  products){
            //pas élégant certe mais efficace
            String produit = product.toString() +": " + String.valueOf(product.getPrix()- product.getPrixAchat());
            productWithBenefit.add(produit);
        }
    return productWithBenefit;
    }

    @ApiOperation(value = "Retourne tous les produits de la base classé par ordre alphabétique.")
    @GetMapping(value = "/GetProductByName")
    public List<Product> trierProduitsParOrdreAlphabetique(){
        List<Product> productByName = productDao.findByOrderByNomAsc();

        checkPrixNull(productByName);

        return productByName;
    }

    /**
     * create error to test
     * @return probably ProduitGratuitException :)
     */
    @ApiOperation(value = "Crée une erreur afin de vérifier qu'on ne peut pas renvoyer un produit avec un prix égal à 0!")
    @GetMapping(value = "/ErreurTest")
    public List<Product> créerUneErreur(){
        List<Product> productByName = productDao.findByOrderByNomAsc();

        //create prix = 0
        if (!productByName.isEmpty()) {
            productByName.get(0).setPrix(0);
        }
        //control and throw exception
        checkPrixNull(productByName);

        return productByName;
    }

    /**
     * check if product got prix == 0
     *
     * @param products products list
     */
    private void checkPrixNull(List<Product> products) {
        for(Product product : products){
            if ( product.getPrix() ==  0 ){
            throw new ProduitGratuitException("Le produit : " + product.getNom() + " possède un prix égal à 0.");
            }
        }
    }


}
