package com.ktam.eshop.view;

import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.repository.ProductRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant; // Added for button styling
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    private final ProductRepository repository;
    private final Grid<ProductEntity> grid = new Grid<>(ProductEntity.class);

    public MainView(ProductRepository repository) {
        this.repository = repository;

        // 1. Configure the data grid
        grid.setColumns("id", "name", "price");
        
        // --- NEW: Add a Delete Button Column ---
        grid.addComponentColumn(product -> {
            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR); // Makes the button red
            
            deleteButton.addClickListener(click -> {
                repository.delete(product); // Delete from MySQL
                updateGrid();               // Refresh the UI
            });
            
            return deleteButton;
        }).setHeader("Actions");
        // ---------------------------------------

        updateGrid(); // Fetch data from MySQL to populate the grid

        // 2. Create the input fields for the form
        TextField nameField = new TextField("Product Name");
        NumberField priceField = new NumberField("Price");
        
        // 3. Create the save button and define its click behavior
        Button saveButton = new Button("Add Product");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Makes the save button blue
        
        saveButton.addClickListener(click -> {
            if (!nameField.isEmpty() && !priceField.isEmpty()) {
                // Save to database
                ProductEntity newProduct = new ProductEntity();
                newProduct.setName(nameField.getValue());
                newProduct.setPrice(priceField.getValue());
                repository.save(newProduct);
                
                // Clear the form and refresh the grid
                nameField.clear();
                priceField.clear();
                updateGrid();
            }
        });

        // 4. Align the form components in a horizontal row
        HorizontalLayout formLayout = new HorizontalLayout(nameField, priceField, saveButton);
        formLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        // 5. Add the form and the grid to the main vertical layout
        add(formLayout, grid);
    }

    // Helper method to fetch the latest data from MySQL
    private void updateGrid() {
        grid.setItems(repository.findAll());
    }
}