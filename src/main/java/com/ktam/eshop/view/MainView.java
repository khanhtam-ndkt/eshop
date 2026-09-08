package com.ktam.eshop.view;

import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.service.ProductService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant; // Added for button styling
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
// import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

import jakarta.validation.ConstraintViolationException;

@Route("")
public class MainView extends VerticalLayout {

    private final ProductService service;
    private final Grid<ProductEntity> grid = new Grid<>(ProductEntity.class);

    public MainView(ProductService service) {
        this.service = service;

        // 1. Configure the data grid
        grid.setColumns("id", "name", "price");

        // --- Delete Button Column ---
        grid.addComponentColumn(product -> {
            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR); // Makes the button red

            deleteButton.addClickListener(click -> {
                service.delete(product.getId());
                updateGrid();
            });

            return deleteButton;
        }).setHeader("Actions");
        // ----------------------------

        updateGrid(); // Fetch data to populate the grid

        // 2. Create the input fields for the form
        TextField nameField = new TextField("Product Name");
        NumberField priceField = new NumberField("Price");

        // 2b. Bind the fields to the entity so the same @NotBlank/@DecimalMin
        // constraints used by JPA are enforced client-side before we ever
        // call the service. writeBean() below returns false / throws when
        // validation fails, so bad input never reaches repository.save().
        BeanValidationBinder<ProductEntity> binder = new BeanValidationBinder<>(ProductEntity.class);
        binder.forField(nameField).bind("name");
        binder.forField(priceField).bind("price");

        // 3. Create the save button and define its click behavior
        Button saveButton = new Button("Add Product");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Makes the save button blue

        saveButton.addClickListener(click -> {
            ProductEntity candidate = new ProductEntity();
            try {
                binder.writeBean(candidate);
            } catch (ValidationException e) {
                Notification n = Notification.show("Please fix the highlighted fields before saving.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                service.create(candidate.getName(), candidate.getPrice());

                // Clear the form and refresh the grid
                binder.readBean(null); // resets both bound fields
                updateGrid();
            } catch (ConstraintViolationException e) {
                // Safety net in case something bypasses the binder (e.g. a
                // constraint that only Hibernate knows about) so the raw
                // exception never reaches the user.
                Notification n = Notification.show("Could not save product: invalid data.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        // 4. Align the form components in a horizontal row
        HorizontalLayout formLayout = new HorizontalLayout(nameField, priceField, saveButton);
        formLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        // 5. Add the form and the grid to the main vertical layout
        add(formLayout, grid);
    }

    // Helper method to fetch the latest data
    private void updateGrid() {
        grid.setItems(service.findAll());
    }
}