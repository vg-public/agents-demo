---
description: "Use when: adding input validation to DTOs or customizing validation messages — creates custom validators, cross-field validation, and Jakarta Bean Validation patterns."
agent: "agent"
tools: [read, edit, search]
argument-hint: "What to validate — e.g., 'Add phone number validation to Customer DTO' or 'Create custom @ValidDateRange annotation'"
---

# Add Validation

Add or enhance **Jakarta Bean Validation** on request DTOs and create custom validators.

## Standard Annotations

```java
public record CreateProductRequest(

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z]{2,5}-\\d{3,6}$", message = "SKU must match format: XX-000 (e.g., ELEC-001)")
    String sku,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 digits and 2 decimal places")
    BigDecimal price,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    Long categoryId,

    @NotEmpty(message = "At least one tag is required")
    @Size(max = 10, message = "At most 10 tags allowed")
    List<@NotBlank String> tags,

    @Email(message = "Invalid email format")
    String contactEmail,

    @Past(message = "Manufacture date must be in the past")
    LocalDate manufactureDate
) {}
```

## Custom Validator — Cross-Field Validation

### Annotation

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String startField();
    String endField();
}
```

### Validator

```java
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.startField();
        this.endField = annotation.endField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(obj);
            LocalDate start = (LocalDate) wrapper.getPropertyValue(startField);
            LocalDate end = (LocalDate) wrapper.getPropertyValue(endField);
            if (start == null || end == null) return true;  // null handled by @NotNull
            return end.isAfter(start);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Usage

```java
@ValidDateRange(startField = "startDate", endField = "endDate")
public record DateRangeRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
```

## Custom Single-Field Validator

```java
// Annotation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhone {
    String message() default "Invalid phone number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator
public class PhoneNumberValidator implements ConstraintValidator<ValidPhone, String> {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{9,14}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;  // Use @NotNull for required
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

## Validation Groups (for different rules on create vs. update)

```java
public interface OnCreate {}
public interface OnUpdate {}

public record ProductRequest(
    @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class)
    Long id,

    @NotBlank(groups = OnCreate.class)
    String name
) {}

// Controller
@PostMapping
public ResponseEntity<?> create(@Validated(OnCreate.class) @RequestBody ProductRequest req) { ... }

@PutMapping("/{id}")
public ResponseEntity<?> update(@Validated(OnUpdate.class) @RequestBody ProductRequest req) { ... }
```

## Rules

- Always use `@Valid` on `@RequestBody` in controllers — validation does not run without it
- Custom validators should return `true` for `null` values — use `@NotNull` separately for required checks
- Error messages should be user-friendly — not technical
- Keep validation on DTOs, not on entities (validate at the boundary)
- Use `@Validated` (Spring) instead of `@Valid` (Jakarta) when using validation groups
