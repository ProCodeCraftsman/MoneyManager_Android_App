# The Design System: Editorial Finance

## 1. Overview & Creative North Star: "The Private Ledger"
This design system moves away from the aggressive, high-friction interfaces typical of fintech. Our Creative North Star is **The Private Ledger**—an experience that feels like a bespoke, high-end physical journal. It rejects the "app-y" look of heavy borders and loud buttons in favor of an editorial layout that breathes.

We achieve a signature look by prioritizing **tonal depth over structural lines**. The interface should feel like layered sheets of premium cardstock. By using intentional asymmetry—such as placing a large, serifed balance stat off-center against a wide margin—we create a sense of calm, curated authority.

---

## 2. Colors: Tonal Atmosphere
Our palette is rooted in organic, low-contrast warmth. We avoid "pure" whites and blacks to reduce eye strain and establish a sophisticated, heritage feel.

### Surface Hierarchy & Nesting
Forget the grid. Use the **Surface Scale** to create "nested" importance. 
- **Surface (Paper):** The foundation. Everything begins here.
- **Surface-Container-Low:** Use for large, secondary sections (e.g., a background for a list of transactions).
- **Surface-Container-Highest:** Use for the most important interactive elements (e.g., an active Card).

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section off content. Boundaries must be defined solely through background color shifts. If a section needs to end, transition from `surface-container-low` to `surface`.

### Glass & Gradient Transitions
To prevent the UI from feeling "flat," use subtle radial gradients on Primary CTAs (transitioning from `primary` to `primary-container`). For floating elements like the Bottom Navigation, use **Glassmorphism**: a semi-transparent `surface` color with a 20px backdrop blur.

---

## 3. Typography: Editorial Authority
We pair a high-contrast Serif with a functional Sans-Serif to create an "Editorial Finance" aesthetic.

- **Display & Headlines (Newsreader/DM Serif Display):** These are our "Statement" pieces. Large balances and section headers should use these to feel like a premium magazine title. Use `display-lg` for total net worth to command attention.
- **Body & Titles (Manrope/DM Sans):** Our workhorse. Used for transactional data and UI labels. It provides a clean, modern counter-balance to the serif headers.
- **The Monospace Exception:** All currency amounts must use **DM Mono**. This ensures that numbers align vertically in lists, providing a "ledger" feel that is both functional and aesthetically nostalgic.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too "digital" for this system. We use **Tonal Layering** to convey height.

- **The Layering Principle:** To "lift" a card, do not add a shadow. Instead, place a `surface-container-lowest` (#ffffff) card on top of a `surface-dim` (#e4d8c6) background.
- **Ambient Shadows:** Only for floating modals. Use a `12%` opacity shadow tinted with the `primary` hue (rather than grey) with a `32px` blur. It should feel like a soft glow, not a shadow.
- **The "Ghost Border" Fallback:** If a border is required for accessibility (e.g., in high-contrast mode), use `outline-variant` at **15% opacity**. A 100% opaque border is a failure of the system.

---

## 5. Components

### Cards (The "Ledger" Cards)
Cards do not have borders. They are defined by their background color (`surface-container`). 
- **The Signature Accent:** To distinguish between accounts, use a **4px top-weighted accent bar** using the `secondary` (Income), `primary` (Expense), or `tertiary` (Gold) tokens. This is the only "hard" line allowed in the UI.

### Buttons & Pills
- **Primary CTA:** Large, `xl` (0.75rem) roundedness. Use a subtle vertical gradient.
- **Pill Filters:** Use `full` (9999px) roundedness. An unselected pill should be the same color as the background, distinguished only by a `ghost-border`. Selected pills should be `primary`.
- **Bottom Navigation:** A floating "island" design. Semi-transparent `surface` with backdrop blur. No top border; use an ambient shadow to separate it from the content scrolling behind it.

### Inputs & Lists
- **Input Fields:** Underline-style inputs are forbidden. Use a soft `surface-container-low` block with `md` roundedness.
- **Lists:** No divider lines between transactions. Use `16px` of vertical whitespace to separate items. Group items by date using a `label-sm` header in `muted-text`.

---

## 6. Do’s and Don’ts

### Do:
- **Do** embrace white space. If a screen feels "empty," it’s working.
- **Do** use `Newsreader` (Serif) for any number over 24pt.
- **Do** use `surface-container` shifts to group related items.
- **Do** ensure Dark Mode maintains the same low-contrast "warmth" by using #141210 as the base, never #000000.

### Don't:
- **Don't** use 1px solid borders to create grids.
- **Don't** use standard grey shadows.
- **Don't** use "Alert Red" for negative expenses. Use our refined `primary` (#a13000) to maintain the sophisticated palette.
- **Don't** center-align long lists of data. Keep the "Ledger" feel with strong left-aligned typography.