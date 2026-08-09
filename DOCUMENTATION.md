# Palm Farm Manager - Complete Function Documentation

## Table of Contents
1. [Overview](#overview)
2. [Authentication & Security](#authentication--security)
3. [Production Management](#production-management)
4. [Task & Wage Management](#task--wage-management)
5. [Financial Management](#financial-management)
6. [Analytics & Reports](#analytics--reports)
7. [Settings & Configuration](#settings--configuration)

---

## Overview

Palm Farm Manager is a comprehensive offline Android application for managing palm farm operations in Cameroon. The app tracks production, finances, workers, and generates detailed analytics and reports.

**Key Features:**
- Complete offline operation (no internet required)
- Production cycle management (annual cycles)
- Worker and task management
- Automatic wage calculations
- Financial tracking (income, expenses, loans, depreciation)
- Real-time stock management
- Comprehensive analytics and charts
- PDF report generation

---

## Authentication & Security

### Biometric Authentication
**Function:** Secure app access using fingerprint authentication
- Uses Android BiometricPrompt API
- Falls back to password authentication if biometrics unavailable
- Password stored encrypted using AndroidX Security Crypto

### Password Authentication
**Function:** Alternative authentication method
- Password hashed and stored securely
- Encrypted storage using AndroidX Security Crypto
- No plaintext passwords stored

**Formula:** Password validation
```
Password Match = Hash(Input Password) == Stored Hash
```

---

## Production Management

### Production Cycles

**Function:** Manages annual production cycles automatically

**Cycle Creation:**
- Automatically creates new cycle when current cycle ends
- Cycle duration: 1 year (365 days)
- Tracks realized bunches per cycle

**Realized Bunches Calculation:**
```
Realized Bunches = Σ(All Harvests in Cycle)
```

### Harvest Management

**Function:** Records palm fruit harvests

**Data Tracked:**
- Harvest number (sequential per cycle)
- Date of harvest
- Number of bunches harvested
- Cycle association

**Bunches Available Calculation:**
```
Total Harvested = Σ(Harvest.numberOfBunches) for cycle
Total Milled = Σ(Milling.bunchesMilled) for cycle
Total Bunches Sold = Σ(Sale.quantity where unit = "BUNCH") for cycle
Total Tonnes Sold = Σ(Sale.quantity where unit = "TONNE") for cycle
Tonnes as Bunches = Total Tonnes Sold × Tonnage Conversion Factor
Total Sold = Total Bunches Sold + Tonnes as Bunches

Bunches Available = Total Harvested - Total Milled - Total Sold
Bunches Available = max(0, Bunches Available)  // Cannot be negative
```

**Note:** Tonnage conversion factor is configurable in app settings (default: number of bunches per tonne).

### Milling Operations

**Function:** Records milling operations that convert bunches to palm oil

**Data Tracked:**
- Date of milling
- Number of bunches milled
- Number of drums cooked
- Oil produced (in gallons)
- Miller (worker) assigned

**Validation:**
```
Bunches to Mill ≤ Bunches Available
```

**Oil Stock Calculation:**
```
Total Oil Produced = Σ(Milling.oilProducedGallons) for all cycles
Total Oil Sold = Σ(Sale.quantity where unit = "GALLON") for all cycles
Total Oil Consumed = Σ(Consumption.quantityGallons) for all cycles

Oil Stock = Total Oil Produced - Total Oil Sold - Total Oil Consumed
Oil Stock = max(0, Oil Stock)  // Cannot be negative
```

**Oil Per Bunch Calculation:**
```
Total Oil Produced (Cycle) = Σ(Milling.oilProducedGallons) for cycle
Total Bunches Milled (Cycle) = Σ(Milling.bunchesMilled) for cycle

Oil Per Bunch (Liters) = (Total Oil Produced × 20) / Total Bunches Milled
```

**Note:** Oil is stored in gallons but displayed as liters. Conversion: 1 gallon = 20 liters.

**Oil Per Drum Calculation:**
```
Total Oil Produced (Cycle) = Σ(Milling.oilProducedGallons) for cycle
Total Drums Cooked (Cycle) = Σ(Milling.drumsCooked) for cycle

Oil Per Drum (Gallons) = Total Oil Produced / Total Drums Cooked
```

**Bunches Per Drum Calculation:**
```
Total Bunches Milled (Cycle) = Σ(Milling.bunchesMilled) for cycle
Total Drums Cooked (Cycle) = Σ(Milling.drumsCooked) for cycle

Bunches Per Drum = Total Bunches Milled / Total Drums Cooked
```

### Loose Nuts Picking

**Function:** Records collection of loose palm nuts

**Data Tracked:**
- Date of picking
- Number of bags collected
- Cycle association

---

## Task & Wage Management

### Task Management

**Function:** Assigns work tasks to workers with quantity tracking

**Task States:**
- **Pending:** Task created but not started
- **In Progress:** Task started but not completed
- **Completed:** Task finished with quantity recorded

**Task Data:**
- Worker assignment
- Task description
- Quantity (nullable - required for wage calculation)
- Pay rate per unit
- Status
- Cycle association

### Wage Calculation

**Function:** Automatically calculates wages from completed tasks

**Gross Wage Calculation:**
```
Completed Unpaid Tasks = Tasks where:
  - status = "COMPLETED"
  - paidInWagePaymentId = null
  - quantity > 0
  - cycleId = current cycle

Gross Wage = Σ(Task.quantity × Task.payRate) for completed unpaid tasks
```

**Advance Payment Tracking:**
```
Total Advances = Σ(AdvancePayment.amount) for worker in cycle
Advances Applied = Σ(WagePayment.totalAdvances) for worker in cycle
Outstanding Advances = max(0, Total Advances - Advances Applied)
```

**Advance Payment Validation:**
```
Maximum Advance = Gross Wage × 0.5  // 50% limit

If Advance Amount > Maximum Advance:
  Reject advance payment
```

**Net Wage Calculation:**
```
Advance Applied = min(Outstanding Advances, Gross Wage)
Net Wage = Gross Wage - Advance Applied
Net Wage = max(0, Net Wage)  // Cannot be negative
```

**Outstanding Wages Calculation:**
```
For each worker:
  Unpaid Tasks = Tasks where:
    - status = "COMPLETED"
    - paidInWagePaymentId = null
    - quantity > 0
    - cycleId = cycle
  
  Gross = Σ(Unpaid Task.quantity × Unpaid Task.payRate)
  Outstanding Advances = Total Advances - Advances Applied
  Net = Gross - min(Outstanding Advances, Gross)
  
Total Outstanding Wages = Σ(Net) for all workers
```

**Wage Payment Processing:**
1. Select completed unpaid tasks for a worker
2. Calculate gross wage from selected tasks
3. Calculate outstanding advances
4. Apply advances to gross wage (up to gross amount)
5. Calculate net payment
6. Create WagePayment record (immutable)
7. Mark selected tasks as paid (link to WagePayment)
8. Generate PDF payslip

---

## Financial Management

### Income Management

#### Sales

**Function:** Records sales of oil, bunches, or tonnes

**Sale Types:**
- **GALLON:** Oil sales (in gallons)
- **BUNCH:** Direct bunch sales
- **TONNE:** Tonne sales (converted to bunches)

**Sale Calculation:**
```
Total Amount = Quantity × Unit Price
```

**Validation:**
```
For GALLON sales:
  Sale Quantity ≤ Oil Stock Available

For BUNCH sales:
  Sale Quantity ≤ Bunches Available

For TONNE sales:
  Validated based on available stock (converted to bunches)
```

**Total Sales Income:**
```
Total Sales = Σ(Sale.totalAmount) for period
```

#### Consumption

**Function:** Records internal oil consumption (not sold)

**Consumption Calculation:**
```
Consumption Value = Quantity (gallons) × Valued At Price
```

**Validation:**
```
Consumption Quantity ≤ Oil Stock Available
```

**Total Consumption Income:**
```
Total Consumption Value = Σ(Consumption.quantityGallons × Consumption.valuedAtPrice) for period
```

**Total Income:**
```
Total Income = Total Sales + Total Consumption Value
```

### Expense Management

**Function:** Tracks operational expenses by category

**Expense Categories:**
- Custom categories (user-defined)
- Examples: Fuel, Maintenance, Supplies, etc.

**Total Expenses:**
```
Total Expenses = Σ(Expense.amount) for period
```

### Fixed Costs & Depreciation

**Function:** Tracks fixed assets with automatic depreciation

**Fixed Cost Types:**
- **RENT:** Rental payments (land, equipment)
- **PURCHASE:** Asset purchases

**Fixed Cost Categories:**
- **LAND:** Land-related costs
- **EQUIPMENT:** Equipment purchases

**Monthly Depreciation Calculation:**
```
Monthly Depreciation = Fixed Cost Amount / (Life Span Years × 12)
```

**Used Value Calculation:**
```
Months Elapsed = (Current Date - Purchase Date) / 30 days
Total Months = Life Span Years × 12

If Months Elapsed ≥ Total Months:
  Used Value = Fixed Cost Amount  // Fully depreciated
Else:
  Used Value = Monthly Depreciation × Months Elapsed
```

**Value Left Calculation:**
```
Value Left = Fixed Cost Amount - Used Value
Value Left = max(0, Value Left)  // Cannot be negative
```

**Depreciation Percentage:**
```
Depreciation % = (Used Value / Fixed Cost Amount) × 100
```

**Total Depreciation (for period):**
```
For each Fixed Cost:
  If Purchase Date ≤ Period End Date:
    Months Elapsed = (Period End Date - Purchase Date) / 30
    Total Months = Life Span Years × 12
    
    If Months Elapsed ≥ Total Months:
      Depreciation = Fixed Cost Amount
    Else:
      Depreciation = (Fixed Cost Amount / Total Months) × Months Elapsed

Total Depreciation = Σ(Depreciation) for all fixed costs
```

**Fixed Assets Value (Balance Sheet):**
```
For each Fixed Cost:
  Current Value = Fixed Cost Amount - Used Value
  Current Value = max(0, Current Value)

Total Fixed Assets = Σ(Current Value) for all fixed costs
```

### Loan Management

**Function:** Tracks loans with simple interest calculations

**Loan Calculation:**
```
Total Interest = Principal × (Interest Rate / 100)
Total Owed = Principal + Total Interest
Monthly Payment = Total Owed / Period (months)
End Date = Start Date + Period (months)
```

**Loan Payment Tracking:**
```
Payments Made = Number of payments recorded
Total Paid = Payments Made × Monthly Payment
Total Left = Total Owed - Total Paid
Total Left = max(0, Total Left)  // Cannot be negative

If Total Left ≤ 0:
  Loan is Fully Paid
```

**Loan Payment Recording:**
```
New Payments Made = Current Payments Made + 1
New Total Left = Total Owed - (Monthly Payment × New Payments Made)

If New Total Left ≤ 0:
  Loan is Fully Paid
  Total Left = 0
```

**Total Loan Debt:**
```
Total Loan Debt = Σ(Loan.totalLeft) for active loans
```

**Loan Payments in Expenses:**
```
Loan Payments Expense = Σ(Loan.monthlyPayment × Loan.numberOfPaymentsMade) for active loans
```

### Advance Payments

**Function:** Records advance payments to workers

**Advance Payment Rules:**
- Cannot exceed 50% of gross wage
- Deducted from final wage payment
- Tracked per worker per cycle

**Total Advances:**
```
Total Advances = Σ(AdvancePayment.amount) for period
```

### Cash Transactions

**Function:** Tracks all cash inflows and outflows

**Transaction Types:**
- Income transactions (sales, consumption)
- Expense transactions (expenses, wages, advances)
- Automatically created when recording financial activities

**Cash Balance:**
```
Cash Balance = Σ(CashTransaction.amount) where:
  - Income transactions: positive amounts
  - Expense transactions: negative amounts
```

---

## Analytics & Reports

### Production KPIs

**Bunches Per Tree:**
```
All-Time Bunches Per Tree = Total Bunches Harvested (Current Cycle) / Total Palms
Current Bunches Per Tree = Bunches from Last Harvest / Total Palms

Percentage Change = ((Current - All-Time) / All-Time) × 100
```

**Oil Per Bunch:**
```
All-Time Oil Per Bunch = (Total Oil Produced (Current Cycle) × 20) / Total Bunches Milled (Current Cycle)
Current Oil Per Bunch = (Oil from Last Milling × 20) / Bunches from Last Milling

Percentage Change = ((Current - All-Time) / All-Time) × 100
```

**Note:** Oil per bunch is displayed in liters (gallons × 20).

### Financial KPIs

**Cost Per Bunch:**
```
Total Costs = Total Expenses + Total Wages + Total Advances
Total Bunches Harvested = Σ(Harvest.numberOfBunches) for current cycle

Cost Per Bunch = Total Costs / Total Bunches Harvested
```

**Income Per Bunch:**
```
Total Income = Total Sales + Total Consumption Value
Total Bunches Harvested = Σ(Harvest.numberOfBunches) for current cycle

Income Per Bunch = Total Income / Total Bunches Harvested
```

**Profit Per Bunch:**
```
Total Costs = Total Expenses + Total Wages + Total Advances
Total Income = Total Sales + Total Consumption Value
Total Bunches Harvested = Σ(Harvest.numberOfBunches) for current cycle

Profit Per Bunch = (Total Income - Total Costs) / Total Bunches Harvested
```

**Profit Margin:**
```
Total Revenue = Total Sales + Total Consumption Value
Total Costs = Total Expenses + Total Wages + Total Advances

Profit Margin % = ((Total Revenue - Total Costs) / Total Revenue) × 100
```

**Return on Investment (ROI):**
```
Total Revenue = Total Sales + Total Consumption Value
Total Costs = Total Expenses + Total Wages + Total Advances

ROI % = ((Total Revenue - Total Costs) / Total Costs) × 100
```

**Net Profit:**
```
Net Profit = Total Revenue - Total Costs
```

### Cash Flow Report

**Function:** Monthly cash flow analysis

**Monthly Income:**
```
Monthly Income = Σ(Sale.totalAmount) for month
                + Σ(Consumption.quantityGallons × Consumption.valuedAtPrice) for month
```

**Monthly Expenses:**
```
Monthly Expenses = Σ(Expense.amount) for month
                  + Σ(WagePayment.netPayment) for month
                  + Σ(AdvancePayment.amount) for month
                  + Σ(Loan.monthlyPayment × Loan.numberOfPaymentsMade) for active loans
                  + Monthly Depreciation (for all fixed costs)
```

**Monthly Net Cash Flow:**
```
Net Cash Flow = Monthly Income - Monthly Expenses
```

**Cumulative Balance:**
```
Cumulative Balance (Month N) = Σ(Net Cash Flow) from first month to Month N
```

**Total Cash Flow:**
```
Total Income = Σ(Monthly Income) for period
Total Expenses = Σ(Monthly Expenses) for period
Net Cash Flow = Total Income - Total Expenses
```

### Profitability Report

**Function:** Comprehensive profitability analysis

**Total Income:**
```
Total Income = Total Sales + Total Consumption Value
```

**Operational Expenses:**
```
Operational Expenses = Total Expenses
                      + Total Wages (net payments + advances)
                      + Loan Payments (monthly payment × payments made)
```

**Depreciation:**
```
Total Depreciation = Σ(Depreciation) for all fixed costs in period
```

**Gross Margin:**
```
Gross Margin = Total Income - Operational Expenses
```

**EBITDA (Earnings Before Interest, Taxes, Depreciation, Amortization):**
```
EBITDA = Gross Margin  // Same as gross margin (no interest/taxes tracked)
```

**Net Balance:**
```
Net Balance = Total Income - Operational Expenses - Total Depreciation
```

**Gross Margin Percentage:**
```
Gross Margin % = (Gross Margin / Total Income) × 100
```

**Net Margin Percentage:**
```
Net Margin % = (Net Balance / Total Income) × 100
```

**Benefit to Cost Ratio:**
```
Benefit to Cost Ratio = Total Income / Operational Expenses
```

**Profit Rate:**
```
Profit Rate % = ((Benefit to Cost Ratio - 1) × 100)
```

### Balance Sheet

**Function:** Assets, liabilities, and net worth calculation

**Current Assets:**

**Oil Stock Value:**
```
Oil Stock Value = Oil Stock (gallons) × Last Sale Price (per gallon)
```

**Bunches Available Value:**
```
Bunches Available Value = Bunches Available × Oil Per Bunch × Last Sale Price / 20
```

**Note:** Conversion factor of 20 converts gallons to liters for oil per bunch calculation.

**Cash:**
```
Cash = Current Cash Balance
```

**Total Current Assets:**
```
Total Current Assets = Oil Stock Value + Bunches Available Value + Cash
```

**Fixed Assets:**
```
Fixed Assets = Σ(Fixed Cost Amount - Used Value) for all fixed costs
```

**Total Assets:**
```
Total Assets = Total Current Assets + Fixed Assets
```

**Liabilities:**

**Loans Payable:**
```
Loans Payable = Σ(Loan.totalLeft) for active loans
```

**Wages Payable:**
```
Wages Payable = Outstanding Wages (unpaid completed tasks)
```

**Total Liabilities:**
```
Total Liabilities = Loans Payable + Wages Payable
```

**Net Worth:**
```
Net Worth = Total Assets - Total Liabilities
```

### Analytics Charts

**Function:** Visual representation of production and financial data

**Chart Periods:**
- Last 6 Months
- Last 12 Months
- This Year
- All Time

**Income vs Expenses Chart:**
```
For each month in period:
  Monthly Income = Σ(Sale.totalAmount) + Σ(Consumption.quantityGallons × Consumption.valuedAtPrice)
  Monthly Expenses = Σ(Expense.amount) + Σ(WagePayment.netPayment) + Σ(AdvancePayment.amount)
```

**Production Trends Chart:**
```
For each month in period:
  Bunches Harvested = Σ(Harvest.numberOfBunches)
  Bunches Milled = Σ(Milling.bunchesMilled)
  Oil Produced = Σ(Milling.oilProducedGallons)
```

**Bunches Harvested Chart:**
```
For each month in period:
  Bunches = Σ(Harvest.numberOfBunches)
```

**Oil Produced Chart:**
```
For each month in period:
  Oil (gallons) = Σ(Milling.oilProducedGallons)
```

**Oil per Bunch Ratio Chart:**
```
For each month in period:
  Oil Per Bunch = Oil Produced (gallons) / Bunches Milled
```

**Cost vs Income per Bunch Chart:**
```
For each month in period:
  Total Costs = Σ(Expense.amount) + Σ(WagePayment.netPayment) + Σ(AdvancePayment.amount)
  Total Income = Σ(Sale.totalAmount) + Σ(Consumption.quantityGallons × Consumption.valuedAtPrice)
  Bunches Harvested = Σ(Harvest.numberOfBunches)
  
  Cost Per Bunch = Total Costs / Bunches Harvested
  Income Per Bunch = Total Income / Bunches Harvested
```

**Cost vs Income per Gallon Chart:**
```
For each month in period:
  Total Costs = Σ(Expense.amount) + Σ(WagePayment.netPayment) + Σ(AdvancePayment.amount)
  Total Income = Σ(Sale.totalAmount) + Σ(Consumption.quantityGallons × Consumption.valuedAtPrice)
  Oil Produced = Σ(Milling.oilProducedGallons)
  
  Cost Per Gallon = Total Costs / Oil Produced
  Income Per Gallon = Total Income / Oil Produced
```

**Cumulative Cost vs Income Chart (Break-Even Analysis):**
```
Cumulative Cost (Month N) = Σ(Monthly Costs) from first month to Month N
Cumulative Income (Month N) = Σ(Monthly Income) from first month to Month N

Break-Even Point = Month where Cumulative Income = Cumulative Cost
```

**Expense Breakdown Pie Chart:**
```
For each expense category:
  Category Total = Σ(Expense.amount) where category matches
  
For Wages:
  Wages Total = Σ(WagePayment.netPayment) + Σ(AdvancePayment.amount)
```

**Cost Analysis Bar Chart:**
```
For each expense category:
  Category Amount = Σ(Expense.amount) where category matches
  
Sorted by amount (descending)
```

**Profitability Multi-Bar Chart:**
```
For each month in period:
  Revenue = Σ(Sale.totalAmount) + Σ(Consumption.quantityGallons × Consumption.valuedAtPrice)
  Costs = Σ(Expense.amount) + Σ(WagePayment.netPayment) + Σ(AdvancePayment.amount)
  Profit = Revenue - Costs
```

---

## Settings & Configuration

### Enterprise Settings

**Function:** Configure farm and enterprise information

**Settings:**
- Enterprise Name
- Tonnage Conversion Factor (bunches per tonne)
- Other farm-specific configurations

### Farm Management

**Function:** Manage farm information

**Farm Data:**
- Farm name
- Number of palms per farm
- Multiple farms supported

**Total Palms:**
```
Total Palms = Σ(Farm.numberOfPalms) for all active farms
```

### Worker Management

**Function:** Manage worker information

**Worker Data:**
- Full name
- Phone number
- Specialties (multiple)
- Active status

**Worker Specialties:**
- HARVESTER
- MILLER
- LOOSE_NUTS_PICKER
- Other custom specialties

### Data Backup & Restore

**Function:** Export and import complete database

**Backup:**
- Exports entire SQLite database
- Includes all data (production, finances, workers, etc.)
- Creates timestamped backup file

**Restore:**
- Imports database from backup file
- Replaces current database
- Validates database integrity before restore

---

## Data Relationships

### Production Cycle Relationships
```
ProductionCycle (1) ──→ (Many) Harvest
ProductionCycle (1) ──→ (Many) Milling
ProductionCycle (1) ──→ (Many) Task
ProductionCycle (1) ──→ (Many) LooseNutsPicking
```

### Worker Relationships
```
Worker (1) ──→ (Many) Task
Worker (1) ──→ (Many) WagePayment
Worker (1) ──→ (Many) AdvancePayment
Worker (1) ──→ (Many) Milling (as miller)
```

### Financial Relationships
```
Sale ──→ Creates CashTransaction (income)
Consumption ──→ Creates CashTransaction (income)
Expense ──→ Creates CashTransaction (expense)
WagePayment ──→ Creates CashTransaction (expense)
AdvancePayment ──→ Creates CashTransaction (expense)
```

### Task-Wage Relationships
```
Task (Many) ──→ (1) WagePayment
Task.paidInWagePaymentId links to WagePayment.id
```

---

## Important Notes

### Unit Conversions
- **Oil Storage:** Stored in gallons, displayed in liters (1 gallon = 20 liters)
- **Oil Per Bunch:** Calculated and displayed in liters
- **Tonnage:** Converted to bunches using tonnage conversion factor

### Validation Rules
- Cannot sell/consume more oil than available stock
- Cannot mill more bunches than available
- Advance payments limited to 50% of gross wage
- Tasks must have quantity > 0 to be included in wage calculation
- Only completed unpaid tasks are included in wage calculations

### Data Integrity
- WagePayment records are immutable (cannot be edited/deleted)
- Tasks linked to WagePayment cannot be modified
- Production cycles automatically created when current cycle ends
- Stock calculations prevent negative values

### Period Calculations
- **Current Cycle:** Data from current production cycle only
- **Month:** Current calendar month
- **Quarter:** Current calendar quarter (3 months)
- **Year:** Current calendar year
- **All Time:** All data from all cycles

---

## Version History

**Version 3.0:**
- Added cumulative cost/income chart for break-even analysis
- Added 5 new analytics charts (bunches only, oil only, ratios, per-unit metrics)
- Database migration for schema updates
- Enhanced analytics with more detailed metrics

**Version 2.0:**
- Added CashTransaction entity for cash flow tracking
- Enhanced financial reporting

**Version 1.0:**
- Initial release with core functionality

---

**Document Version:** 1.0  
**Last Updated:** January 2026  
**App Version:** 3.0
