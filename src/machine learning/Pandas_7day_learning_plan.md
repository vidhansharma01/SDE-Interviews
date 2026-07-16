# 🐼 Pandas 7-Day Learning Plan
### *A Senior Software Engineer's Guide to Mastering Pandas*

> **Prerequisites:** Basic Python knowledge (lists, dicts, loops, functions), pip installed.
> **Goal:** Go from zero to production-ready pandas skills in 7 days.

---

## 📦 Setup (Before Day 1)

```bash
pip install pandas numpy matplotlib seaborn jupyter
jupyter notebook  # or use VS Code with Jupyter extension
```

---

## Day 1 — Core Data Structures: Series & DataFrame

### 🎯 Learning Objectives
- Understand the two core pandas primitives: `Series` and `DataFrame`
- Create DataFrames from dicts, lists, CSV, and JSON
- Navigate and inspect a DataFrame

### 📚 Concepts

#### Series — 1D labeled array
```python
import pandas as pd

s = pd.Series([10, 20, 30], index=['a', 'b', 'c'])
print(s['b'])       # 20
print(s.dtype)      # int64
print(s.values)     # array([10, 20, 30])
```

#### DataFrame — 2D labeled table
```python
data = {
    'name': ['Alice', 'Bob', 'Charlie'],
    'age':  [30, 25, 35],
    'salary': [90000, 70000, 120000]
}
df = pd.DataFrame(data)
```

#### Key Inspection Methods
```python
df.head(5)          # First 5 rows
df.tail(3)          # Last 3 rows
df.shape            # (rows, cols)
df.dtypes           # Column data types
df.info()           # Memory + null counts
df.describe()       # Statistical summary
df.columns          # Column names
df.index            # Row index
```

#### Reading Data
```python
df = pd.read_csv('data.csv')
df = pd.read_json('data.json')
df = pd.read_excel('data.xlsx', sheet_name='Sheet1')
df = pd.read_sql(query, connection)   # from a DB
```

### 🏋️ Practice Tasks
1. Load a CSV from [Kaggle Titanic Dataset](https://www.kaggle.com/c/titanic/data) or any public dataset.
2. Print `shape`, `dtypes`, and `describe()` of the dataset.
3. Create a `Series` from a Python list with a custom string index.
4. Create a `DataFrame` from a dictionary of your choice.

### 💡 Senior Engineer Tips
- Always call `df.info()` first — it reveals nulls, dtypes, and memory in one shot.
- Use `df.describe(include='all')` to include categorical columns.
- Prefer `pd.read_csv(..., dtype={'col': str})` to avoid silent type coercions on load.

---

## Day 2 — Selecting, Filtering & Indexing

### 🎯 Learning Objectives
- Select columns and rows using `[]`, `.loc[]`, `.iloc[]`
- Filter rows with boolean conditions
- Understand the index and how to reset/set it

### 📚 Concepts

#### Column Selection
```python
df['name']                          # Series
df[['name', 'salary']]              # DataFrame
```

#### Row Selection
```python
df.loc[0]                           # By label
df.iloc[0]                          # By integer position
df.loc[0:2, 'name':'age']           # Label-based slice
df.iloc[0:3, 0:2]                   # Position-based slice
```

#### Boolean Filtering
```python
df[df['age'] > 28]
df[(df['age'] > 25) & (df['salary'] > 80000)]
df[df['name'].isin(['Alice', 'Bob'])]
df[df['name'].str.startswith('A')]
```

#### Index Management
```python
df.set_index('name', inplace=True)
df.reset_index(inplace=True)
df.sort_index()
```

#### `.query()` Method — Cleaner Filtering
```python
df.query("age > 28 and salary > 80000")
```

### 🏋️ Practice Tasks
1. Filter all rows where a numeric column exceeds its mean.
2. Select every other row using `iloc`.
3. Set a string column as the index and access rows by label.
4. Use `.query()` to replicate a compound boolean filter.

### 💡 Senior Engineer Tips
- Always prefer `.loc[]` over `[]` when assigning values — avoids `SettingWithCopyWarning`.
- Use `.query()` in pipelines for readability; it also supports `@variable` syntax for external variables.
- `.isin()` is O(n) — for very large sets consider using a set + `apply` or merge-based filtering.

---

## Day 3 — Data Cleaning & Handling Missing Values

### 🎯 Learning Objectives
- Detect, fill, and drop missing values
- Change data types
- Remove duplicates and clean strings

### 📚 Concepts

#### Detecting Nulls
```python
df.isnull().sum()                   # Null count per column
df.isnull().mean() * 100            # % missing
df[df['age'].isnull()]              # Rows with null age
```

#### Handling Nulls
```python
df.dropna()                         # Drop rows with ANY null
df.dropna(subset=['age'])           # Drop rows where 'age' is null
df.dropna(thresh=3)                 # Keep rows with at least 3 non-nulls

df['age'].fillna(df['age'].mean(), inplace=True)    # Mean imputation
df['name'].fillna('Unknown', inplace=True)
df.ffill()                          # Forward fill
df.bfill()                          # Backward fill
```

#### Type Conversion
```python
df['age'] = df['age'].astype(int)
df['date'] = pd.to_datetime(df['date'])
df['price'] = pd.to_numeric(df['price'], errors='coerce')  # NaN on failure
```

#### Duplicates
```python
df.duplicated().sum()
df.drop_duplicates(inplace=True)
df.drop_duplicates(subset=['name', 'age'])
```

#### String Cleaning
```python
df['name'] = df['name'].str.strip()
df['name'] = df['name'].str.lower()
df['name'] = df['name'].str.replace(r'\s+', ' ', regex=True)
df['email'] = df['email'].str.extract(r'([\w.-]+@[\w.-]+)')
```

### 🏋️ Practice Tasks
1. Find the column with the highest % of missing values in a dataset.
2. Impute numeric nulls with median and categorical nulls with mode.
3. Convert a mixed-format date column to `datetime`.
4. Remove all duplicate rows and confirm with `.duplicated().sum()`.

### 💡 Senior Engineer Tips
- Never blindly `dropna()` — understand *why* data is missing (MCAR, MAR, MNAR).
- `pd.to_numeric(..., errors='coerce')` is safer than `astype(float)` — it won't crash on bad data.
- Document your cleaning assumptions as comments; data cleaning is where most bugs hide.

---

## Day 4 — GroupBy, Aggregation & Pivot Tables

### 🎯 Learning Objectives
- Group data and compute aggregations with `groupby`
- Use `agg()` for multi-function aggregations
- Build pivot tables and crosstabs

### 📚 Concepts

#### GroupBy Basics
```python
df.groupby('department')['salary'].mean()
df.groupby('department')['salary'].agg(['mean', 'median', 'std'])
```

#### Multi-Column GroupBy
```python
df.groupby(['department', 'level'])['salary'].mean()
```

#### Custom Aggregations
```python
df.groupby('department').agg(
    avg_salary=('salary', 'mean'),
    head_count=('name', 'count'),
    max_age=('age', 'max')
)
```

#### Transform — Broadcast aggregation back to original shape
```python
df['dept_avg_salary'] = df.groupby('department')['salary'].transform('mean')
df['salary_rank'] = df.groupby('department')['salary'].rank(ascending=False)
```

#### Pivot Tables
```python
pd.pivot_table(
    df,
    values='salary',
    index='department',
    columns='level',
    aggfunc='mean',
    fill_value=0
)
```

#### Crosstab — Frequency tables
```python
pd.crosstab(df['department'], df['level'], normalize='index')
```

### 🏋️ Practice Tasks
1. Group by a categorical column and compute mean, max, and count.
2. Use `transform('mean')` to add a "group average" column next to original data.
3. Build a pivot table with 2 categorical columns.
4. Find the top earner per department using `groupby` + `idxmax`.

### 💡 Senior Engineer Tips
- `groupby` returns a `GroupBy` object — it's lazy. Computation happens only when you call an aggregation.
- Use `observed=True` when grouping on `Categorical` columns to avoid empty groups.
- `transform` is crucial for feature engineering in ML pipelines.

---

## Day 5 — Merging, Joining & Reshaping Data

### 🎯 Learning Objectives
- Combine DataFrames using `merge`, `join`, and `concat`
- Reshape with `melt`, `pivot`, `stack`, and `unstack`

### 📚 Concepts

#### Merging (SQL-style joins)
```python
# Inner join (default)
pd.merge(df1, df2, on='user_id')

# Left join
pd.merge(df1, df2, on='user_id', how='left')

# Merge on different column names
pd.merge(df1, df2, left_on='user_id', right_on='id', how='inner')

# Merge on index
pd.merge(df1, df2, left_index=True, right_index=True)
```

#### Concatenating
```python
pd.concat([df1, df2], axis=0, ignore_index=True)   # Stack rows
pd.concat([df1, df2], axis=1)                       # Stack columns
```

#### Melt — Wide to Long
```python
pd.melt(df,
        id_vars=['name'],
        value_vars=['Q1', 'Q2', 'Q3', 'Q4'],
        var_name='quarter',
        value_name='revenue')
```

#### Pivot — Long to Wide
```python
df.pivot(index='name', columns='quarter', values='revenue')
```

#### Stack / Unstack
```python
df.stack()      # Column headers -> innermost row index
df.unstack()    # Innermost row index -> column headers
```

### 🏋️ Practice Tasks
1. Perform left, right, inner, and outer joins between two DataFrames. Count rows in each.
2. Melt a wide quarterly report into a long format.
3. Concatenate 3 DataFrames vertically and reset the index.
4. Identify rows with no match after a left join (where right-side column is null).

### 💡 Senior Engineer Tips
- Always check `len()` before and after merges — unexpected row explosions reveal duplicate keys.
- Use `validate='one_to_one'` in `pd.merge()` to enforce key uniqueness — it will raise on violations.
- Prefer `pd.concat` over `df.append()` — `append` was deprecated in pandas 2.0.

---

## Day 6 — Apply, Lambda, Time Series & Performance

### 🎯 Learning Objectives
- Apply custom logic with `apply`, `map`, and `applymap`
- Work with datetime data and time series
- Understand vectorization and performance best practices

### 📚 Concepts

#### apply / map
```python
# Apply a function to each element (Series)
df['name'].map(str.upper)
df['age'].map(lambda x: 'Senior' if x > 30 else 'Junior')

# Apply a function row-wise or column-wise (DataFrame)
df.apply(lambda row: row['salary'] / row['age'], axis=1)

# Apply to every element in DataFrame
df[['salary', 'bonus']].map(lambda x: round(x, 2))
```

#### Vectorized Alternatives (faster than apply)
```python
# Prefer this over apply for arithmetic
df['tax'] = df['salary'] * 0.3
df['name_upper'] = df['name'].str.upper()

# np.where for conditional logic
import numpy as np
df['level'] = np.where(df['salary'] > 100000, 'Senior', 'Junior')
```

#### Time Series
```python
df['date'] = pd.to_datetime(df['date'])
df.set_index('date', inplace=True)

df['year']  = df.index.year
df['month'] = df.index.month
df['dow']   = df.index.day_name()

# Resampling
df['salary'].resample('M').mean()     # Monthly average
df['salary'].resample('Q').sum()      # Quarterly sum

# Rolling windows
df['rolling_7d'] = df['salary'].rolling(window=7).mean()
df['ewm_salary'] = df['salary'].ewm(span=7).mean()

# Shifting
df['prev_salary'] = df['salary'].shift(1)
df['salary_change'] = df['salary'] - df['salary'].shift(1)
```

#### Performance Tips
```python
# Check memory usage
df.memory_usage(deep=True)

# Downcast dtypes
df['age'] = pd.to_numeric(df['age'], downcast='integer')

# Use categories for low-cardinality strings
df['department'] = df['department'].astype('category')

# Avoid loops — use vectorized ops
# Bad (slow):
for i, row in df.iterrows():
    df.at[i, 'tax'] = row['salary'] * 0.3

# Good (fast):
df['tax'] = df['salary'] * 0.3
```

### 🏋️ Practice Tasks
1. Use `apply` to create a custom bucketing column (e.g., salary tiers).
2. Refactor an `apply` solution to use `np.where` or vectorized ops. Benchmark both.
3. Load a time-series dataset (e.g., stock prices), resample to weekly averages.
4. Compute a 7-day rolling average and plot it.

### 💡 Senior Engineer Tips
- `iterrows()` is 100-1000x slower than vectorized ops. Never use it for large DataFrames.
- Use `category` dtype for string columns with <100 unique values — saves 5-10x memory.
- `.dt` accessor and `.str` accessor are vectorized — always prefer them over `apply` for string/date ops.

---

## Day 7 — Real-World Pipeline: EDA to Insights

### 🎯 Learning Objectives
- Build an end-to-end data analysis pipeline
- Connect pandas to visualization libraries
- Apply everything in a realistic scenario

### 📚 Full Pipeline Template

```python
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# --- 1. LOAD -----------------------------------------------------------
df = pd.read_csv('data.csv')

# --- 2. INSPECT --------------------------------------------------------
print(df.shape)
print(df.dtypes)
print(df.describe(include='all'))
print(df.isnull().mean().sort_values(ascending=False))

# --- 3. CLEAN ----------------------------------------------------------
df.drop_duplicates(inplace=True)
df.columns = df.columns.str.lower().str.replace(' ', '_')
df['date'] = pd.to_datetime(df['date'], errors='coerce')
df['price'] = pd.to_numeric(df['price'], errors='coerce')
df['category'] = df['category'].astype('category')
df['price'].fillna(df['price'].median(), inplace=True)

# --- 4. FEATURE ENGINEER -----------------------------------------------
df['year'] = df['date'].dt.year
df['month'] = df['date'].dt.month
df['revenue_tier'] = pd.cut(df['price'],
                             bins=[0, 50, 200, float('inf')],
                             labels=['Low', 'Mid', 'High'])

# --- 5. AGGREGATE & ANALYSE --------------------------------------------
summary = df.groupby('category').agg(
    total_revenue=('price', 'sum'),
    avg_price=('price', 'mean'),
    item_count=('price', 'count')
).sort_values('total_revenue', ascending=False)

monthly_trend = df.set_index('date')['price'].resample('M').sum()

# --- 6. VISUALISE -------------------------------------------------------
fig, axes = plt.subplots(1, 2, figsize=(14, 5))

summary['total_revenue'].plot(kind='bar', ax=axes[0], color='steelblue')
axes[0].set_title('Revenue by Category')
axes[0].set_ylabel('Total Revenue')

monthly_trend.plot(ax=axes[1], color='darkorange', linewidth=2)
axes[1].set_title('Monthly Revenue Trend')

plt.tight_layout()
plt.savefig('analysis.png', dpi=150)
plt.show()

# --- 7. EXPORT ---------------------------------------------------------
summary.to_csv('summary_report.csv')
df.to_parquet('cleaned_data.parquet', index=False)  # Efficient storage
```

### 🏋️ Final Project
Pick **one** of these datasets and build a complete analysis notebook:

| Dataset | Source | Focus Area |
|---|---|---|
| Titanic | Kaggle | Classification prep, survival analysis |
| NYC Airbnb | Inside Airbnb | Geo analysis, price modeling |
| COVID-19 | Our World in Data | Time series, rolling averages |
| Superstore Sales | Kaggle | Business EDA, pivot tables |

**Deliverable:** A Jupyter Notebook with:
- [ ] Data loading and inspection
- [ ] Cleaning with documented assumptions
- [ ] At least 3 `groupby` aggregations
- [ ] At least 1 merge or reshape operation
- [ ] At least 2 visualizations
- [ ] 3 key insights written in plain English

### 💡 Senior Engineer Tips
- Prefer **Parquet** over CSV for intermediate files — 5-10x smaller, preserves dtypes.
- Use **method chaining** for clean, readable pipelines:
  ```python
  result = (df
      .dropna(subset=['price'])
      .assign(revenue_tier=pd.cut(df['price'], bins=[0, 50, 200, float('inf')]))
      .groupby('revenue_tier')['price']
      .agg(['mean', 'count'])
  )
  ```
- Pin your pandas version in `requirements.txt` — the API has breaking changes between major versions.

---

## 📊 7-Day Summary

| Day | Topic | Key Methods |
|---|---|---|
| 1 | Data Structures | `read_csv`, `head`, `info`, `describe` |
| 2 | Selection & Filtering | `loc`, `iloc`, `query`, boolean masks |
| 3 | Data Cleaning | `fillna`, `dropna`, `astype`, `drop_duplicates` |
| 4 | GroupBy & Aggregation | `groupby`, `agg`, `transform`, `pivot_table` |
| 5 | Merging & Reshaping | `merge`, `concat`, `melt`, `pivot` |
| 6 | Apply & Performance | `apply`, `map`, `resample`, vectorization |
| 7 | End-to-End Pipeline | Full EDA workflow, visualization, export |

---

## 📖 Resources

| Type | Resource |
|---|---|
| Official Docs | [pandas.pydata.org/docs](https://pandas.pydata.org/docs/) |
| Cheat Sheet | [Pandas Cheat Sheet (DataCamp)](https://www.datacamp.com/cheat-sheet/pandas-cheat-sheet-for-data-science-in-python) |
| Practice | [Kaggle Pandas Course](https://www.kaggle.com/learn/pandas) |
| Book | *Python for Data Analysis* by Wes McKinney (pandas creator) |
| Dataset Hub | [Kaggle Datasets](https://www.kaggle.com/datasets) |

---

## ⚠️ Common Pitfalls to Avoid

1. **`SettingWithCopyWarning`** — Always use `.loc[]` when setting values, not chained indexing.
2. **Silent type coercion on read** — Always specify `dtype` for critical columns in `read_csv`.
3. **`iterrows()` in loops** — Replace with vectorized operations or `numpy`.
4. **Ignoring index after merge/concat** — Always check if you need `ignore_index=True`.
5. **Modifying original DataFrames** — Use `.copy()` when slicing to avoid mutating source data.
6. **`append()` deprecated** — Use `pd.concat()` in pandas >= 2.0.

---

*Happy data wrangling! 🐼*
