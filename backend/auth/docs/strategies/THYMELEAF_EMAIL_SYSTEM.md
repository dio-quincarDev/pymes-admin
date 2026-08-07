# 📧 Thymeleaf Email System (Component-Based)

This document describes the design system and architecture used to render responsive HTML emails using Thymeleaf templates.

---

## 🏗️ Service Architecture

The system segregates template processing from transit logic via two services:

1. **`EmailTemplateService`**: Renders the `.html` templates located under `templates/` utilizing Spring Boot's `SpringTemplateEngine` to inject dynamic context variables.
2. **`EmailService`**: Serves as the gateway façade using `JavaMailSender` to send MimeMessage HTML emails.

---

## 🎨 Layout & Design System

The emails share a layout-based structure designed to match the frontend palette:

### Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | `#A3785E` | Buttons, CTA borders |
| Secondary | `#E2E8E4` | Primary body text |
| Accent | `#71837F` | Secondary text, borders |
| Dark | `#1B2624` | Inner cards, container backgrounds |
| Dark Page | `#0B1210` | Outer email background |
| Positive | `#2D5A27` | Success labels |
| Warning | `#C5A059` | Warning text boxes |

### Template Structure

* **`_base.html`**: Root responsive layout wrapper enforcing mobile responsiveness (max-width: 600px) and font styling (Inter).
* **Components**: Reusable parts in `templates/components/`:
  - `_header.html`: Branding header
  - `_footer.html`: Terms, privacy, and social footers
  - `_button.html`: Centered call-to-action button
  - `_alert.html`: Warning box
  - `_divider.html`: Visual separator line
* **Emails**: Specific templates in `templates/emails/`:
  - `verification.html`: Account validation link
  - `password-reset.html`: Password recovery link
  - `invitation.html`: Member workplace invitation
