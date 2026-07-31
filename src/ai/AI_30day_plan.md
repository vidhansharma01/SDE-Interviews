# 🤖 Artificial Intelligence — 30-Day Learning Plan
### *A Senior Software Engineer's Complete Roadmap to Modern AI*

> **Prerequisites:** Python proficiency, basic math (linear algebra, probability, calculus), familiarity with NumPy/pandas.
> **Goal:** Build a comprehensive, production-ready AI skillset spanning classical ML, deep learning, NLP, computer vision, LLMs, and AI agents.
> **Time Commitment:** ~2–3 hours/day.

---

## 📦 Environment Setup (Before Day 1)

```bash
# Create isolated environment
python -m venv ai_env
source ai_env/bin/activate          # Windows: ai_env\Scripts\activate

# Core stack
pip install numpy pandas matplotlib seaborn scikit-learn
pip install torch torchvision torchaudio
pip install transformers datasets accelerate peft
pip install openai langchain langchain-community chromadb
pip install fastapi uvicorn python-dotenv
pip install jupyter notebook ipywidgets wandb optuna
pip install opencv-python pillow
pip install gym stable-baselines3

# Verify GPU
python -c "import torch; print('CUDA:', torch.cuda.is_available())"
```

---

## 🗺️ Learning Roadmap Overview

```
Week 1: AI Foundations & Classical ML
Week 2: Deep Learning & Neural Architectures  
Week 3: NLP, Computer Vision & Generative AI
Week 4: LLMs, AI Agents, MLOps & Production AI
```

---

## 🗓️ Week 1 — AI Foundations & Classical Machine Learning (Days 1–7)

---

### Day 1 — What is AI? History, Landscape & Mindset

#### 🎯 Objectives
- Understand the full AI landscape and where each subfield fits
- Set the right mental model for 30-day learning

#### 📚 The AI Hierarchy

```
Artificial Intelligence (AI)
├── Machine Learning (ML)
│   ├── Supervised Learning
│   ├── Unsupervised Learning
│   └── Reinforcement Learning
│       └── Deep Learning (DL)
│           ├── CNNs (Vision)
│           ├── RNNs / Transformers (Sequence)
│           └── Generative Models (GANs, Diffusion, VAEs)
├── Knowledge Representation & Reasoning
├── Planning & Search (A*, MCTS)
├── Natural Language Processing (NLP)
├── Computer Vision (CV)
└── Robotics
```

#### AI Subfield Decision Map
| Problem Type | Recommended Approach |
|---|---|
| Structured/tabular data | XGBoost, LightGBM, Random Forest |
| Image classification | CNN, ResNet, ViT |
| Text classification | BERT, fine-tuned LLM |
| Text generation | GPT, LLaMA fine-tune |
| Anomaly detection | Isolation Forest, Autoencoder |
| Recommendation | Matrix Factorization, Two-Tower |
| Time series | LSTM, Temporal Fusion Transformer |
| Game playing / Control | Reinforcement Learning (PPO, DQN) |
| Search & retrieval | Embeddings + Vector DB (RAG) |

#### Key AI Milestones Timeline
| Year | Milestone |
|---|---|
| 1956 | "Artificial Intelligence" coined at Dartmouth |
| 1986 | Backpropagation popularized |
| 1997 | Deep Blue beats Kasparov at chess |
| 2012 | AlexNet — deep learning revolution begins |
| 2017 | Transformer architecture ("Attention Is All You Need") |
| 2020 | GPT-3 — large language model era |
| 2022 | ChatGPT — AI goes mainstream |
| 2023–24 | Multimodal AI, AI agents, LLM proliferation |

#### 💡 Senior Engineer Tips
- AI is a tool, not magic. Always ask: "Is ML/AI actually the right tool here?"
- The best AI engineers have strong software engineering foundations first.
- Follow AI news via [arXiv](https://arxiv.org), [Papers with Code](https://paperswithcode.com/), and [Hugging Face Blog](https://huggingface.co/blog).

---

### Day 2 — Python for AI: NumPy, Pandas & Visualization

#### 🎯 Objectives
- Master the numerical Python stack used in every AI project
- Build intuition for data exploration before modeling

#### 📚 NumPy — Vectorized Computation

```python
import numpy as np

# Array creation
arr = np.array([1, 2, 3, 4, 5])
mat = np.random.randn(100, 10)      # 100 samples, 10 features

# Vectorized operations (always prefer over loops)
mean_vec = mat.mean(axis=0)         # Feature means
std_vec  = mat.std(axis=0)          # Feature std devs
normalized = (mat - mean_vec) / std_vec  # Z-score normalization

# Broadcasting
weights = np.array([0.1, 0.5, 0.3, 0.05, 0.02, 0.01, 0.01, 0.005, 0.005, 0.002])
weighted = mat * weights            # (100, 10) * (10,) — broadcast

# Linear algebra
A = np.random.randn(5, 5)
eigenvalues, eigenvectors = np.linalg.eig(A)
inv_A = np.linalg.inv(A)
dot_product = A @ A.T
```

#### Pandas — Data Wrangling

```python
import pandas as pd

df = pd.read_csv('data.csv')

# EDA pipeline
print(df.info())                        # Dtypes + nulls
print(df.describe(include='all'))       # Statistics
print(df.isnull().mean().sort_values(ascending=False))  # Missing %

# Feature engineering
df['age_bucket'] = pd.cut(df['age'], bins=[0, 25, 40, 65, 100],
                           labels=['Young', 'Adult', 'Middle', 'Senior'])
df['income_log'] = np.log1p(df['income'])    # Log transform skewed feature
df['date'] = pd.to_datetime(df['date'])
df['day_of_week'] = df['date'].dt.day_name()
```

#### Visualization for AI

```python
import matplotlib.pyplot as plt
import seaborn as sns

fig, axes = plt.subplots(2, 2, figsize=(12, 10))

# Distribution of target
axes[0, 0].hist(df['target'], bins=30, color='steelblue', edgecolor='black')
axes[0, 0].set_title('Target Distribution')

# Feature correlations
sns.heatmap(df.corr(numeric_only=True), annot=True, cmap='coolwarm', ax=axes[0, 1])
axes[0, 1].set_title('Correlation Matrix')

# Pairplot for feature relationships
# sns.pairplot(df, hue='target')  # Use on smaller datasets

plt.tight_layout()
plt.savefig('eda.png', dpi=150)
```

---

### Day 3 — Supervised Learning: Regression

#### 🎯 Objectives
- Build regression models from scratch and with scikit-learn
- Understand key regression metrics and diagnostics

#### 📚 Linear Regression

```python
import numpy as np
from sklearn.linear_model import LinearRegression, Ridge, Lasso, ElasticNet
from sklearn.preprocessing import StandardScaler, PolynomialFeatures
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
from sklearn.model_selection import train_test_split, cross_val_score

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Standard pipeline
pipeline = Pipeline([
    ('scaler', StandardScaler()),
    ('poly', PolynomialFeatures(degree=2, include_bias=False)),
    ('model', Ridge(alpha=1.0))
])

pipeline.fit(X_train, y_train)
y_pred = pipeline.predict(X_test)

# Metrics
rmse = np.sqrt(mean_squared_error(y_test, y_pred))
mae  = mean_absolute_error(y_test, y_pred)
r2   = r2_score(y_test, y_pred)
print(f"RMSE: {rmse:.4f} | MAE: {mae:.4f} | R²: {r2:.4f}")

# Cross-validation
cv_scores = cross_val_score(pipeline, X, y, cv=5, scoring='neg_rmse')
print(f"CV RMSE: {-cv_scores.mean():.4f} ± {cv_scores.std():.4f}")
```

#### Regression Algorithm Comparison
| Algorithm | Handles Nonlinearity | Handles Outliers | Interpretable |
|---|---|---|---|
| Linear Regression | No | Poor | Yes |
| Ridge (L2) | No | Moderate | Yes |
| Lasso (L1) | No | Moderate | Yes (sparse) |
| Polynomial | Yes | Poor | Moderate |
| Random Forest | Yes | Good | Partial |
| XGBoost | Yes | Good | With SHAP |

---

### Day 4 — Supervised Learning: Classification

#### 🎯 Objectives
- Master classification algorithms and evaluation metrics
- Handle class imbalance effectively

#### 📚 Classification with scikit-learn

```python
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.metrics import (classification_report, confusion_matrix,
                              roc_auc_score, average_precision_score,
                              ConfusionMatrixDisplay)
from sklearn.model_selection import StratifiedKFold

# Logistic Regression — always your baseline
lr = Pipeline([('scaler', StandardScaler()),
               ('model', LogisticRegression(max_iter=1000))])

# Random Forest — strong ensemble
rf = RandomForestClassifier(n_estimators=200, max_depth=8,
                             min_samples_leaf=5, random_state=42, n_jobs=-1)

# Gradient Boosting — often best on tabular data
from xgboost import XGBClassifier
xgb = XGBClassifier(n_estimators=300, learning_rate=0.05,
                    max_depth=6, subsample=0.8,
                    colsample_bytree=0.8, use_label_encoder=False,
                    eval_metric='logloss')

# Evaluation
y_pred = rf.fit(X_train, y_train).predict(X_test)
y_prob = rf.predict_proba(X_test)[:, 1]
print(classification_report(y_test, y_pred))
print(f"ROC-AUC: {roc_auc_score(y_test, y_prob):.4f}")

# Class imbalance handling
from imblearn.over_sampling import SMOTE
X_resampled, y_resampled = SMOTE(random_state=42).fit_resample(X_train, y_train)
```

#### Classification Metrics Cheat Sheet
| Metric | Formula | Use When |
|---|---|---|
| Accuracy | TP+TN / All | Balanced classes |
| Precision | TP / (TP+FP) | FP is costly (spam filter) |
| Recall | TP / (TP+FN) | FN is costly (disease detection) |
| F1 | 2*P*R/(P+R) | Balance precision/recall |
| ROC-AUC | Area under ROC | Ranking quality, imbalanced data |
| PR-AUC | Area under PR curve | Severely imbalanced datasets |

---

### Day 5 — Unsupervised Learning: Clustering & Dimensionality Reduction

#### 🎯 Objectives
- Apply clustering algorithms and evaluate without labels
- Reduce dimensions for visualization and feature engineering

#### 📚 Clustering

```python
from sklearn.cluster import KMeans, DBSCAN, AgglomerativeClustering
from sklearn.mixture import GaussianMixture
from sklearn.metrics import silhouette_score

# K-Means — find optimal k with elbow method
inertias = []
sil_scores = []
K_range = range(2, 11)

for k in K_range:
    km = KMeans(n_clusters=k, random_state=42, n_init=10)
    labels = km.fit_predict(X_scaled)
    inertias.append(km.inertia_)
    sil_scores.append(silhouette_score(X_scaled, labels))

best_k = K_range[np.argmax(sil_scores)]
print(f"Best K by silhouette: {best_k}")

# DBSCAN — finds arbitrary shape clusters, handles noise
dbscan = DBSCAN(eps=0.5, min_samples=5)
labels = dbscan.fit_predict(X_scaled)
n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
n_noise = list(labels).count(-1)
print(f"Clusters: {n_clusters}, Noise points: {n_noise}")
```

#### Dimensionality Reduction

```python
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
import umap

# PCA — linear, fast, interpretable
pca = PCA(n_components=50)
X_pca = pca.fit_transform(X_scaled)
print(f"Explained variance (50 components): {pca.explained_variance_ratio_.sum():.2%}")

# t-SNE — nonlinear, for visualization only (2D/3D)
tsne = TSNE(n_components=2, perplexity=30, random_state=42)
X_2d = tsne.fit_transform(X_pca)    # Run on PCA-reduced data for speed

# UMAP — faster than t-SNE, preserves global structure better
reducer = umap.UMAP(n_components=2, random_state=42)
X_umap = reducer.fit_transform(X_scaled)

# Plot clusters
plt.figure(figsize=(10, 6))
plt.scatter(X_umap[:, 0], X_umap[:, 1], c=labels, cmap='tab10', s=5)
plt.colorbar()
plt.title('UMAP Projection with Cluster Labels')
plt.show()
```

---

### Day 6 — Model Selection, Evaluation & Feature Engineering

#### 🎯 Objectives
- Prevent data leakage with proper cross-validation
- Build a robust feature engineering pipeline

#### 📚 Proper Model Evaluation

```python
from sklearn.model_selection import (GridSearchCV, RandomizedSearchCV,
                                      StratifiedKFold, TimeSeriesSplit)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer

# ColumnTransformer — apply different transforms to different column types
numeric_features = ['age', 'income', 'tenure']
categorical_features = ['city', 'plan_type', 'device']

numeric_pipeline = Pipeline([
    ('imputer', SimpleImputer(strategy='median')),
    ('scaler', StandardScaler())
])

categorical_pipeline = Pipeline([
    ('imputer', SimpleImputer(strategy='most_frequent')),
    ('encoder', OneHotEncoder(handle_unknown='ignore', sparse_output=False))
])

preprocessor = ColumnTransformer([
    ('num', numeric_pipeline, numeric_features),
    ('cat', categorical_pipeline, categorical_features)
])

# Full pipeline with model
full_pipeline = Pipeline([
    ('preprocessor', preprocessor),
    ('model', XGBClassifier())
])

# Hyperparameter search
param_dist = {
    'model__n_estimators': [100, 200, 300],
    'model__max_depth': [3, 5, 7],
    'model__learning_rate': [0.01, 0.05, 0.1],
    'model__subsample': [0.7, 0.8, 0.9]
}

search = RandomizedSearchCV(full_pipeline, param_dist, n_iter=50,
                             cv=StratifiedKFold(5), scoring='roc_auc',
                             n_jobs=-1, random_state=42, verbose=1)
search.fit(X_train, y_train)
print(f"Best ROC-AUC: {search.best_score_:.4f}")
print(f"Best params: {search.best_params_}")
```

---

### Day 7 — Ensemble Methods & Gradient Boosting Mastery

#### 🎯 Objectives
- Deep dive into gradient boosting (XGBoost, LightGBM, CatBoost)
- Understand when ensembles win and how to stack models

#### 📚 Gradient Boosting Deep Dive

```python
import xgboost as xgb
import lightgbm as lgb
from catboost import CatBoostClassifier

# LightGBM — fastest on large datasets
lgb_model = lgb.LGBMClassifier(
    n_estimators=500,
    learning_rate=0.05,
    num_leaves=63,               # Key param: controls complexity
    min_child_samples=20,
    subsample=0.8,
    colsample_bytree=0.8,
    reg_alpha=0.1,               # L1 regularization
    reg_lambda=1.0               # L2 regularization
)

# CatBoost — best for categorical features
cat_model = CatBoostClassifier(
    iterations=500,
    learning_rate=0.05,
    depth=6,
    cat_features=['city', 'plan_type'],   # No encoding needed!
    verbose=100
)

# Model stacking
from sklearn.ensemble import StackingClassifier
estimators = [('lgb', lgb_model), ('cat', cat_model), ('rf', rf)]
stacker = StackingClassifier(
    estimators=estimators,
    final_estimator=LogisticRegression(),
    cv=5, stack_method='predict_proba'
)

# SHAP for interpretability
import shap
explainer = shap.TreeExplainer(lgb_model)
shap_values = explainer.shap_values(X_test)
shap.summary_plot(shap_values[1], X_test, feature_names=feature_names)
```

#### Gradient Boosting Comparison
| Library | Speed | Categorical | GPU | Best For |
|---|---|---|---|---|
| XGBoost | Fast | Manual encoding | Yes | General purpose |
| LightGBM | Fastest | Partial support | Yes | Large datasets |
| CatBoost | Moderate | Native support | Yes | High cardinality cats |

---

## 🗓️ Week 2 — Deep Learning Core (Days 8–14)

---

### Day 8 — Neural Networks: Architecture & Training

#### 🎯 Objectives
- Build and train neural networks with PyTorch
- Understand the full training loop deeply

#### 📚 Complete Training Loop

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

# Model definition
class FeedForwardNN(nn.Module):
    def __init__(self, input_dim, hidden_dims, output_dim, dropout=0.3):
        super().__init__()
        layers = []
        dims = [input_dim] + hidden_dims
        for i in range(len(dims) - 1):
            layers.extend([
                nn.Linear(dims[i], dims[i+1]),
                nn.BatchNorm1d(dims[i+1]),
                nn.ReLU(),
                nn.Dropout(dropout)
            ])
        layers.append(nn.Linear(dims[-1], output_dim))
        self.network = nn.Sequential(*layers)

    def forward(self, x):
        return self.network(x)

# Device-agnostic training
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
model = FeedForwardNN(input_dim=30, hidden_dims=[256, 128, 64], output_dim=1).to(device)

# Optimizer + Scheduler
optimizer = optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-4)
scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=50)
criterion = nn.BCEWithLogitsLoss()

# Training loop with validation
def train_epoch(model, loader, optimizer, criterion):
    model.train()
    total_loss = 0
    for X_batch, y_batch in loader:
        X_batch, y_batch = X_batch.to(device), y_batch.to(device)
        optimizer.zero_grad()
        output = model(X_batch).squeeze()
        loss = criterion(output, y_batch.float())
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)  # Gradient clipping
        optimizer.step()
        total_loss += loss.item()
    return total_loss / len(loader)

@torch.no_grad()
def evaluate(model, loader, criterion):
    model.eval()
    total_loss, preds, targets = 0, [], []
    for X_batch, y_batch in loader:
        X_batch, y_batch = X_batch.to(device), y_batch.to(device)
        output = model(X_batch).squeeze()
        total_loss += criterion(output, y_batch.float()).item()
        preds.extend(torch.sigmoid(output).cpu().numpy())
        targets.extend(y_batch.cpu().numpy())
    return total_loss / len(loader), preds, targets
```

---

### Day 9 — Convolutional Neural Networks (CNNs) for Vision

#### 📚 CNN Architecture

```python
class ModernCNN(nn.Module):
    def __init__(self, num_classes=10):
        super().__init__()
        self.features = nn.Sequential(
            self._conv_block(3, 32),    # 224x224 -> 112x112
            self._conv_block(32, 64),   # 112x112 -> 56x56
            self._conv_block(64, 128),  # 56x56 -> 28x28
            self._conv_block(128, 256), # 28x28 -> 14x14
            nn.AdaptiveAvgPool2d(1)     # Global Average Pooling -> 1x1
        )
        self.head = nn.Sequential(
            nn.Flatten(),
            nn.Linear(256, 512), nn.ReLU(), nn.Dropout(0.5),
            nn.Linear(512, num_classes)
        )

    def _conv_block(self, in_ch, out_ch):
        return nn.Sequential(
            nn.Conv2d(in_ch, out_ch, 3, padding=1, bias=False),
            nn.BatchNorm2d(out_ch),
            nn.ReLU(inplace=True),
            nn.Conv2d(out_ch, out_ch, 3, padding=1, bias=False),
            nn.BatchNorm2d(out_ch),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2)
        )

    def forward(self, x):
        return self.head(self.features(x))

# Transfer Learning — always prefer this over training from scratch
import torchvision.models as models
model = models.efficientnet_b0(pretrained=True)
model.classifier[1] = nn.Linear(model.classifier[1].in_features, num_classes)
```

---

### Day 10 — Recurrent Networks & Sequence Modeling

#### 📚 LSTM for Sequence Tasks

```python
class BiLSTMClassifier(nn.Module):
    def __init__(self, vocab_size, embed_dim, hidden_size, num_classes, num_layers=2):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=0)
        self.lstm = nn.LSTM(embed_dim, hidden_size, num_layers=num_layers,
                            batch_first=True, dropout=0.3, bidirectional=True)
        self.attention = nn.Linear(hidden_size * 2, 1)  # Attention pooling
        self.classifier = nn.Linear(hidden_size * 2, num_classes)
        self.dropout = nn.Dropout(0.3)

    def forward(self, x):
        embedded = self.dropout(self.embedding(x))
        out, _ = self.lstm(embedded)

        # Attention pooling over timesteps
        attn_weights = torch.softmax(self.attention(out), dim=1)
        context = (out * attn_weights).sum(dim=1)
        return self.classifier(self.dropout(context))
```

---

### Day 11 — Transformer Architecture Deep Dive

#### 📚 From Scratch Transformer

```python
import math

class TransformerBlock(nn.Module):
    def __init__(self, d_model=512, num_heads=8, ff_dim=2048, dropout=0.1):
        super().__init__()
        self.attention = nn.MultiheadAttention(d_model, num_heads,
                                               dropout=dropout, batch_first=True)
        self.ff = nn.Sequential(
            nn.Linear(d_model, ff_dim), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(ff_dim, d_model)
        )
        self.norm1 = nn.LayerNorm(d_model)
        self.norm2 = nn.LayerNorm(d_model)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x, mask=None):
        # Pre-LayerNorm (more stable than post-LN)
        attn_out, _ = self.attention(self.norm1(x), self.norm1(x), self.norm1(x))
        x = x + self.dropout(attn_out)
        x = x + self.dropout(self.ff(self.norm2(x)))
        return x

# Positional encoding
class PositionalEncoding(nn.Module):
    def __init__(self, d_model, max_len=5000):
        super().__init__()
        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len).unsqueeze(1).float()
        div_term = torch.exp(torch.arange(0, d_model, 2).float() *
                             (-math.log(10000.0) / d_model))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.register_buffer('pe', pe.unsqueeze(0))

    def forward(self, x):
        return x + self.pe[:, :x.shape[1]]
```

---

### Day 12 — Advanced Training Techniques

#### 📚 Mixed Precision, Gradient Accumulation & Distributed Training

```python
from torch.cuda.amp import autocast, GradScaler

scaler = GradScaler()                   # Automatic loss scaling for FP16

# Mixed precision training loop
model.train()
optimizer.zero_grad()

for step, (X_batch, y_batch) in enumerate(train_loader):
    X_batch, y_batch = X_batch.to(device), y_batch.to(device)

    with autocast():                    # FP16 forward pass
        output = model(X_batch)
        loss = criterion(output, y_batch)
        loss = loss / accumulation_steps  # Gradient accumulation

    scaler.scale(loss).backward()

    if (step + 1) % accumulation_steps == 0:
        scaler.unscale_(optimizer)
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        scaler.step(optimizer)
        scaler.update()
        optimizer.zero_grad()
        scheduler.step()

# Tip: Use HuggingFace Accelerate for easy multi-GPU
from accelerate import Accelerator
accelerator = Accelerator(mixed_precision='fp16')
model, optimizer, train_loader = accelerator.prepare(model, optimizer, train_loader)
```

---

### Day 13 — Generative Models: GANs, VAEs & Diffusion

```python
# Variational Autoencoder (VAE)
class VAE(nn.Module):
    def __init__(self, input_dim=784, latent_dim=64):
        super().__init__()
        self.encoder = nn.Sequential(nn.Linear(input_dim, 512), nn.ReLU(),
                                      nn.Linear(512, 256), nn.ReLU())
        self.mu = nn.Linear(256, latent_dim)
        self.log_var = nn.Linear(256, latent_dim)
        self.decoder = nn.Sequential(nn.Linear(latent_dim, 256), nn.ReLU(),
                                      nn.Linear(256, 512), nn.ReLU(),
                                      nn.Linear(512, input_dim), nn.Sigmoid())

    def reparameterize(self, mu, log_var):
        eps = torch.randn_like(mu)
        return mu + eps * torch.exp(0.5 * log_var)

    def forward(self, x):
        h = self.encoder(x)
        mu, log_var = self.mu(h), self.log_var(h)
        z = self.reparameterize(mu, log_var)
        return self.decoder(z), mu, log_var

# Diffusion model (conceptual)
# 1. Forward: x_0 -> x_T by adding Gaussian noise incrementally
# 2. Reverse: x_T -> x_0 by denoising with a U-Net (learns to predict noise)
# 3. Stable Diffusion: works in latent space (VAE encoder/decoder + U-Net denoiser)
```

---

### Day 14 — Reinforcement Learning Fundamentals

#### 📚 RL Core Concepts

```python
import gym
import numpy as np

env = gym.make('CartPole-v1')

# Q-Learning (tabular)
Q_table = np.zeros([obs_space, action_space])
alpha = 0.1      # Learning rate
gamma = 0.99     # Discount factor
epsilon = 1.0    # Exploration rate

for episode in range(1000):
    state = env.reset()[0]
    done = False
    while not done:
        # Epsilon-greedy policy
        if np.random.random() < epsilon:
            action = env.action_space.sample()    # Explore
        else:
            action = np.argmax(Q_table[state])    # Exploit

        next_state, reward, done, _, _ = env.step(action)

        # Bellman update
        Q_table[state, action] += alpha * (
            reward + gamma * np.max(Q_table[next_state]) - Q_table[state, action]
        )
        state = next_state

    epsilon = max(0.01, epsilon * 0.995)    # Decay exploration

# Stable-Baselines3 for modern RL algorithms
from stable_baselines3 import PPO, DQN, SAC

model = PPO('MlpPolicy', env, verbose=1, n_steps=2048,
             learning_rate=3e-4, batch_size=64)
model.learn(total_timesteps=100_000)
```

#### RL Algorithm Guide
| Algorithm | Type | Action Space | Best For |
|---|---|---|---|
| DQN | Value-based | Discrete | Atari games |
| PPO | Policy gradient | Both | Robotics, games |
| SAC | Actor-Critic | Continuous | Continuous control |
| A3C | Actor-Critic | Both | Async parallel |
| DDPG | Actor-Critic | Continuous | Robotic arms |

---

## 🗓️ Week 3 — NLP, Computer Vision & Generative AI (Days 15–21)

---

### Day 15 — NLP Pipeline: Preprocessing to Embeddings

```python
from transformers import AutoTokenizer

# Tokenization
tokenizer = AutoTokenizer.from_pretrained('bert-base-uncased')
texts = ["AI is transforming the world!", "Machine learning is fascinating."]
encoded = tokenizer(texts, padding=True, truncation=True,
                    max_length=128, return_tensors='pt')

# Word embeddings
from gensim.models import Word2Vec, FastText
sentences = [text.split() for text in corpus]
w2v = Word2Vec(sentences, vector_size=300, window=5, min_count=2, workers=4)
vector = w2v.wv['artificial']           # 300-dim vector for a word

# Sentence embeddings with SentenceTransformers
from sentence_transformers import SentenceTransformer
sbert = SentenceTransformer('all-MiniLM-L6-v2')
embeddings = sbert.encode(texts, batch_size=32, show_progress_bar=True)
# Shape: (2, 384) — ready for similarity search or classification
```

---

### Day 16 — Fine-Tuning BERT for NLP Tasks

```python
from transformers import (AutoTokenizer, AutoModelForSequenceClassification,
                           Trainer, TrainingArguments)
from datasets import load_dataset
import evaluate

# Load dataset
dataset = load_dataset('imdb')
tokenizer = AutoTokenizer.from_pretrained('bert-base-uncased')

def tokenize(batch):
    return tokenizer(batch['text'], truncation=True, padding='max_length', max_length=512)

tokenized = dataset.map(tokenize, batched=True)

# Model
model = AutoModelForSequenceClassification.from_pretrained(
    'bert-base-uncased', num_labels=2
)

# Training
metric = evaluate.load("accuracy")
def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = logits.argmax(axis=-1)
    return metric.compute(predictions=preds, references=labels)

args = TrainingArguments(
    output_dir='./bert-imdb',
    num_train_epochs=3,
    per_device_train_batch_size=16,
    per_device_eval_batch_size=32,
    learning_rate=2e-5,
    weight_decay=0.01,
    evaluation_strategy='epoch',
    save_strategy='epoch',
    load_best_model_at_end=True,
    fp16=True,
    logging_dir='./logs'
)

trainer = Trainer(
    model=model,
    args=args,
    train_dataset=tokenized['train'],
    eval_dataset=tokenized['test'],
    compute_metrics=compute_metrics
)

trainer.train()
trainer.evaluate()
trainer.save_model('./bert-imdb-final')
```

---

### Day 17 — Computer Vision: Object Detection & Segmentation

```python
# Object Detection with Ultralytics YOLO
from ultralytics import YOLO
import cv2

# Load pretrained model
model = YOLO('yolov8n.pt')

# Inference
results = model('image.jpg', conf=0.5, iou=0.45)
for result in results:
    boxes = result.boxes.xyxy.cpu().numpy()       # [x1, y1, x2, y2]
    confs  = result.boxes.conf.cpu().numpy()       # Confidence scores
    classes = result.boxes.cls.cpu().numpy()       # Class IDs
    print(f"Detected {len(boxes)} objects")

# Fine-tune on custom dataset
model = YOLO('yolov8s.pt')
model.train(data='dataset.yaml', epochs=50, imgsz=640, batch=16,
            lr0=0.01, lrf=0.1, optimizer='AdamW')

# Semantic Segmentation with Mask2Former (Hugging Face)
from transformers import Mask2FormerForUniversalSegmentation, Mask2FormerImageProcessor

processor = Mask2FormerImageProcessor.from_pretrained("facebook/mask2former-swin-large-coco-panoptic")
model = Mask2FormerForUniversalSegmentation.from_pretrained("facebook/mask2former-swin-large-coco-panoptic")
```

---

### Day 18 — Large Language Models: Prompting & APIs

```python
from openai import OpenAI

client = OpenAI()

# Basic completion
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "You are a helpful senior software engineer."},
        {"role": "user", "content": "Explain the CAP theorem in simple terms."}
    ],
    temperature=0.3,
    max_tokens=500
)
print(response.choices[0].message.content)

# Structured output (JSON mode)
response = client.chat.completions.create(
    model="gpt-4o",
    response_format={"type": "json_object"},
    messages=[
        {"role": "system", "content": "Return JSON only."},
        {"role": "user", "content": "Extract entities from: 'Apple released iPhone 16 in September 2024'"}
    ]
)

# Function calling
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "Get current weather for a city",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "City name"},
                "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]}
            },
            "required": ["city"]
        }
    }
}]
```

#### Prompting Techniques
| Technique | Description | Best For |
|---|---|---|
| Zero-shot | No examples, just instruction | Simple tasks |
| Few-shot | 2-5 examples in prompt | Classification, extraction |
| Chain-of-Thought | "Think step by step" | Math, reasoning |
| ReAct | Reason + Act in loops | Agents, tool use |
| System Prompt | Persistent persona/context | All applications |

---

### Day 19 — Retrieval-Augmented Generation (RAG)

#### 🎯 Objectives
- Build a RAG pipeline to give LLMs access to private knowledge
- Understand embedding models and vector databases

```python
from langchain.document_loaders import PyPDFLoader, WebBaseLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.embeddings import OpenAIEmbeddings
from langchain.vectorstores import Chroma
from langchain.chat_models import ChatOpenAI
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate

# 1. Load & chunk documents
loader = PyPDFLoader("company_handbook.pdf")
documents = loader.load()

splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    separators=["\n\n", "\n", ".", " "]
)
chunks = splitter.split_documents(documents)
print(f"Created {len(chunks)} chunks")

# 2. Embed & store in vector DB
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma.from_documents(chunks, embeddings, persist_directory="./chroma_db")
vectorstore.persist()

# 3. Create retriever
retriever = vectorstore.as_retriever(
    search_type="mmr",                          # Max Marginal Relevance — diverse results
    search_kwargs={"k": 5, "fetch_k": 20}
)

# 4. RAG chain with custom prompt
template = """Use the following context to answer the question.
If you don't know the answer, say "I don't know." Don't make up answers.

Context:
{context}

Question: {question}

Answer:"""

prompt = PromptTemplate(template=template, input_variables=["context", "question"])
llm = ChatOpenAI(model="gpt-4o", temperature=0)

qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    chain_type="stuff",
    retriever=retriever,
    chain_type_kwargs={"prompt": prompt},
    return_source_documents=True
)

result = qa_chain({"query": "What is the company's vacation policy?"})
print(result['result'])
print("\nSources:", [doc.metadata['source'] for doc in result['source_documents']])
```

#### RAG Architecture
```
User Query
    |
    v
[Embedding Model] --> Query Vector
    |
    v
[Vector DB] --> Top-K Relevant Chunks
    |
    v
[LLM] (Query + Context) --> Grounded Answer
```

---

### Day 20 — AI Agents & Tool Use

#### 🎯 Objectives
- Build an autonomous AI agent that uses tools
- Understand the ReAct (Reason + Act) pattern

```python
from langchain.agents import AgentExecutor, create_react_agent
from langchain.tools import Tool, DuckDuckGoSearchRun
from langchain.tools import tool
from langchain import hub

llm = ChatOpenAI(model="gpt-4o", temperature=0)
search = DuckDuckGoSearchRun()

# Define custom tools
@tool
def calculate(expression: str) -> str:
    """Evaluate a mathematical expression. Input: a valid Python math expression."""
    try:
        return str(eval(expression, {"__builtins__": {}}, {}))
    except Exception as e:
        return f"Error: {e}"

@tool
def get_stock_price(ticker: str) -> str:
    """Get current stock price for a ticker symbol like AAPL, GOOGL."""
    import yfinance as yf
    stock = yf.Ticker(ticker)
    info = stock.info
    return f"{ticker}: ${info.get('currentPrice', 'N/A')} (Market cap: ${info.get('marketCap', 'N/A'):,})"

tools = [
    Tool(name="Search", func=search.run,
         description="Search the web for current information"),
    calculate,
    get_stock_price
]

# Create ReAct agent
prompt = hub.pull("hwchase17/react")
agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True,
                                max_iterations=5, handle_parsing_errors=True)

result = agent_executor.invoke({
    "input": "What is Apple's current stock price? What is 15% of that price?"
})
```

#### Agent Types
| Agent Type | Approach | Best For |
|---|---|---|
| ReAct | Reason then act | Tool use, search |
| Plan-and-Execute | Plan all steps, then execute | Complex multi-step |
| Self-Ask | Decomposes into sub-questions | Research tasks |
| Function Calling | LLM directly calls tools | Structured APIs |
| Multi-Agent | Agents collaborate | Complex workflows |

---

### Day 21 — Multimodal AI & Vision-Language Models

```python
from openai import OpenAI
import base64

client = OpenAI()

# Vision with GPT-4o
def encode_image(path):
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode('utf-8')

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "Describe this image and extract any text you see."},
            {"type": "image_url", "image_url": {
                "url": f"data:image/jpeg;base64,{encode_image('photo.jpg')}",
                "detail": "high"
            }}
        ]
    }]
)

# CLIP — zero-shot image classification
from transformers import CLIPModel, CLIPProcessor
from PIL import Image

model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

image = Image.open("cat.jpg")
labels = ["a cat", "a dog", "a car", "a person"]
inputs = processor(text=labels, images=image, return_tensors="pt", padding=True)

with torch.no_grad():
    outputs = model(**inputs)
    probs = outputs.logits_per_image.softmax(dim=1)

for label, prob in zip(labels, probs[0]):
    print(f"{label}: {prob.item():.3f}")
```

---

## 🗓️ Week 4 — Production AI, MLOps & Advanced Topics (Days 22–30)

---

### Day 22 — LLM Fine-Tuning with LoRA & PEFT

```python
from transformers import AutoModelForCausalLM, AutoTokenizer, TrainingArguments
from peft import LoraConfig, get_peft_model, TaskType
from trl import SFTTrainer
from datasets import Dataset

# Load base model in 4-bit quantization
from transformers import BitsAndBytesConfig

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.float16,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type='nf4'
)

model = AutoModelForCausalLM.from_pretrained(
    "meta-llama/Llama-3.1-8B",
    quantization_config=bnb_config,
    device_map="auto"
)
tokenizer = AutoTokenizer.from_pretrained("meta-llama/Llama-3.1-8B")

# Apply LoRA
lora_config = LoraConfig(
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
    lora_dropout=0.05,
    bias="none",
    task_type=TaskType.CAUSAL_LM
)
model = get_peft_model(model, lora_config)
model.print_trainable_parameters()

# Fine-tune with SFTTrainer (supervised fine-tuning)
trainer = SFTTrainer(
    model=model,
    args=TrainingArguments(
        output_dir='./llama-finetuned',
        num_train_epochs=3,
        per_device_train_batch_size=4,
        gradient_accumulation_steps=4,
        learning_rate=2e-4,
        fp16=True,
        logging_steps=10,
        save_strategy='epoch'
    ),
    train_dataset=train_dataset,
    dataset_text_field='text',
    max_seq_length=2048
)
trainer.train()
model.save_pretrained('./llama-finetuned')
```

---

### Day 23 — Vector Databases & Semantic Search

```python
import chromadb
from sentence_transformers import SentenceTransformer

# ChromaDB — local vector database
client = chromadb.PersistentClient(path="./vector_db")
collection = client.get_or_create_collection(
    name="documents",
    metadata={"hnsw:space": "cosine"}
)

# Add documents
encoder = SentenceTransformer('all-MiniLM-L6-v2')
texts = ["AI is the future", "Machine learning powers recommendation systems", "Python is great for data science"]
embeddings = encoder.encode(texts).tolist()

collection.add(
    embeddings=embeddings,
    documents=texts,
    ids=[f"doc_{i}" for i in range(len(texts))],
    metadatas=[{"source": "blog", "date": "2024"} for _ in texts]
)

# Semantic search
query = "What technology powers Netflix recommendations?"
query_embedding = encoder.encode(query).tolist()
results = collection.query(
    query_embeddings=[query_embedding],
    n_results=3,
    where={"source": "blog"}               # Metadata filtering
)
print(results['documents'])

# Pinecone — managed vector DB for production
import pinecone
pinecone.init(api_key="your-api-key", environment="us-east1-gcp")
index = pinecone.Index("semantic-search")
index.upsert(vectors=[(f"id_{i}", emb, meta) for i, (emb, meta) in enumerate(zip(embeddings, metadatas))])
results = index.query(vector=query_embedding, top_k=5, include_metadata=True)
```

---

### Day 24 — MLOps: Experiment Tracking, Model Registry & CI/CD

```python
import mlflow
import mlflow.pytorch

# Track experiments with MLflow
mlflow.set_tracking_uri("http://localhost:5000")
mlflow.set_experiment("image-classification")

with mlflow.start_run(run_name="resnet50-v2"):
    # Log hyperparameters
    mlflow.log_params({
        "model": "resnet50",
        "lr": 1e-4,
        "batch_size": 64,
        "epochs": 50
    })

    for epoch in range(50):
        train_loss, val_acc = train_epoch(), evaluate()

        # Log metrics
        mlflow.log_metrics({
            "train_loss": train_loss,
            "val_accuracy": val_acc,
            "learning_rate": scheduler.get_last_lr()[0]
        }, step=epoch)

    # Log and register model
    mlflow.pytorch.log_model(model, "model",
                              registered_model_name="image-classifier")
    mlflow.log_artifact("confusion_matrix.png")

# Model versioning
client = mlflow.tracking.MlflowClient()
client.transition_model_version_stage(
    name="image-classifier",
    version=3,
    stage="Production"
)
```

```yaml
# .github/workflows/ml_pipeline.yml
name: ML CI/CD Pipeline
on: [push]
jobs:
  train-and-evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with: { python-version: '3.11' }
      - name: Install dependencies
        run: pip install -r requirements.txt
      - name: Run data validation
        run: python validate_data.py
      - name: Train model
        run: python train.py --config config.yaml
      - name: Evaluate model
        run: python evaluate.py --threshold 0.90
      - name: Push to registry (if threshold met)
        run: python register_model.py
```

---

### Day 25 — Model Serving & Production APIs

```python
from fastapi import FastAPI, File, UploadFile, HTTPException
from pydantic import BaseModel
import torch
import time
import logging

app = FastAPI(title="AI Model API", version="1.0.0")
logger = logging.getLogger("uvicorn")

# Load model at startup
@app.on_event("startup")
async def startup_event():
    global model, tokenizer
    model = load_model("./best_model.pt")
    model.eval()
    logger.info("Model loaded successfully")

class TextRequest(BaseModel):
    text: str
    max_length: int = 512

class PredictionResponse(BaseModel):
    prediction: str
    confidence: float
    latency_ms: float

@app.post("/predict", response_model=PredictionResponse)
async def predict(request: TextRequest):
    start = time.time()
    try:
        with torch.no_grad():
            inputs = tokenizer(request.text, return_tensors="pt",
                               truncation=True, max_length=request.max_length).to(device)
            outputs = model(**inputs)
            probs = torch.softmax(outputs.logits, dim=-1)
            pred_id = probs.argmax().item()
            confidence = probs.max().item()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    latency = (time.time() - start) * 1000
    return PredictionResponse(
        prediction=LABELS[pred_id],
        confidence=round(confidence, 4),
        latency_ms=round(latency, 2)
    )

@app.get("/health")
async def health():
    return {"status": "healthy", "model_loaded": model is not None}
```

---

### Day 26 — AI Safety, Ethics & Responsible AI

#### 🎯 Objectives
- Understand bias, fairness, and hallucination in AI systems
- Apply practical mitigation strategies

```python
# Bias detection in models
from fairlearn.metrics import MetricFrame, demographic_parity_difference
from sklearn.metrics import accuracy_score

# Compute metrics across demographic groups
mf = MetricFrame(
    metrics={"accuracy": accuracy_score, "precision": precision_score},
    y_true=y_test,
    y_pred=y_pred,
    sensitive_features=demographic_data
)
print(mf.by_group)
print("Demographic parity difference:", demographic_parity_difference(y_test, y_pred, sensitive_features=demographic_data))

# Detecting hallucinations in LLM outputs
def check_grounded_response(query: str, context: str, response: str) -> dict:
    """Use a judge LLM to verify if response is grounded in context."""
    prompt = f"""
    Context: {context}
    Question: {query}
    Answer: {response}

    Is the answer factually grounded in the context?
    Answer with JSON: {{"grounded": true/false, "reason": "...", "confidence": 0.0-1.0}}
    """
    result = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        response_format={"type": "json_object"}
    )
    return json.loads(result.choices[0].message.content)
```

#### Responsible AI Checklist
- [ ] **Bias Audit**: Evaluate model across demographic subgroups
- [ ] **Data Governance**: Understand data provenance and consent
- [ ] **Hallucination Mitigation**: Use RAG + grounding checks for LLMs
- [ ] **Model Card**: Document intended use, limitations, training data
- [ ] **Input Validation**: Sanitize and validate all user inputs
- [ ] **Output Filtering**: Screen generated content for harmful outputs
- [ ] **Privacy**: Ensure no PII leaks through model responses
- [ ] **Adversarial Testing**: Red-team your model for prompt injection

---

### Day 27 — AI System Design & Architecture Patterns

#### 🎯 Objectives
- Design production AI systems that scale
- Know the common AI architecture patterns

#### AI System Patterns

```
Pattern 1: Offline Batch Inference
  [Raw Data] -> [Feature Store] -> [Model] -> [Predictions DB] -> [Application]
  Best for: Recommendations, fraud scoring, batch analytics

Pattern 2: Online Real-Time Inference
  [User Request] -> [API Gateway] -> [Feature Service] -> [Model Server] -> [Response]
  Best for: Search ranking, content moderation, NLP APIs

Pattern 3: RAG Architecture
  [Query] -> [Embedding] -> [Vector DB] -> [Retrieved Chunks] -> [LLM] -> [Answer]
  Best for: Enterprise search, document Q&A, knowledge bases

Pattern 4: Multi-Agent Orchestration
  [User] -> [Orchestrator Agent] -> [Planner] -> [Tool Agents] -> [Synthesizer] -> [Response]
  Best for: Complex research, code generation, workflow automation

Pattern 5: Human-in-the-Loop
  [Model Prediction] -> [Confidence Check] -> [High confidence: auto-serve]
                                           -> [Low confidence: human review]
  Best for: Medical AI, legal AI, high-stakes decisions
```

#### Production AI Infrastructure
| Component | Options | Notes |
|---|---|---|
| Model Serving | TorchServe, Triton, vLLM, BentoML | vLLM for LLMs |
| Vector DB | Pinecone, Weaviate, ChromaDB, pgvector | pgvector for simplicity |
| Feature Store | Feast, Tecton, Vertex AI | Prevents training/serving skew |
| Orchestration | Airflow, Prefect, ZenML | For ML pipelines |
| Monitoring | Evidently AI, Whylogs, Arize | Track data/model drift |
| LLM Gateway | LiteLLM, PortKey | Multi-provider, caching, rate-limiting |

---

### Day 28 — AI Performance Optimization & Scaling

```python
# vLLM — high-throughput LLM serving
from vllm import LLM, SamplingParams

llm = LLM(model="meta-llama/Llama-3.1-8B-Instruct",
          tensor_parallel_size=4,         # Across 4 GPUs
          gpu_memory_utilization=0.9)

sampling_params = SamplingParams(temperature=0.7, top_p=0.9, max_tokens=512)
prompts = ["Tell me about AI", "Explain neural networks"]  # Batch!
outputs = llm.generate(prompts, sampling_params)

# Caching for LLM responses
import hashlib
import redis

cache = redis.Redis(host='localhost', port=6379, db=0)

def cached_llm_call(prompt: str, model: str = "gpt-4o") -> str:
    cache_key = hashlib.md5(f"{model}:{prompt}".encode()).hexdigest()
    cached = cache.get(cache_key)
    if cached:
        return cached.decode('utf-8')
    response = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}]
    )
    result = response.choices[0].message.content
    cache.setex(cache_key, 3600, result)     # Cache for 1 hour
    return result

# Speculative decoding (faster LLM inference)
# Small "draft" model generates tokens quickly
# Large model verifies/corrects in parallel
# ~2-3x speedup with same quality
```

---

### Day 29 — Capstone Project: Build an End-to-End AI Application

#### 🎯 Project: Intelligent Document Assistant

Build a complete AI-powered document Q&A system with:

```
Architecture:
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  FastAPI    │────▶│  LangChain   │────▶│  ChromaDB       │
│  Backend    │     │  RAG Chain   │     │  Vector Store   │
└─────────────┘     └──────────────┘     └─────────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐     ┌──────────────┐
│  Redis      │     │  OpenAI/     │
│  Cache      │     │  Local LLM   │
└─────────────┘     └──────────────┘
```

```python
# Project structure
"""
ai_document_assistant/
├── app/
│   ├── api/
│   │   ├── routes/
│   │   │   ├── chat.py
│   │   │   ├── documents.py
│   │   │   └── health.py
│   │   └── middleware.py
│   ├── core/
│   │   ├── config.py
│   │   ├── rag_chain.py
│   │   └── vector_store.py
│   ├── models/
│   │   └── schemas.py
│   └── main.py
├── tests/
│   ├── test_rag.py
│   └── test_api.py
├── docker-compose.yml
├── Dockerfile
└── requirements.txt
"""

# Capstone deliverables
deliverables = [
    "Document upload and chunking pipeline",
    "Semantic search with ChromaDB",
    "RAG-powered Q&A with source attribution",
    "Conversation memory (multi-turn chat)",
    "FastAPI REST endpoints with auth",
    "Streaming response support (SSE)",
    "Redis caching layer",
    "Docker containerization",
    "CI/CD pipeline with GitHub Actions",
    "Evaluation: RAGAS framework for RAG quality metrics"
]
```

---

### Day 30 — Reflection, Roadmap & What's Next

#### 🎯 30-Day Review & Skills Inventory

```
Week 1: AI Foundations & Classical ML
✅ Python stack: NumPy, Pandas, Matplotlib
✅ Supervised: Regression, Classification, Ensembles
✅ Unsupervised: Clustering, Dimensionality Reduction
✅ Model evaluation, pipelines, hyperparameter tuning

Week 2: Deep Learning Core
✅ Neural networks from scratch and with PyTorch
✅ CNNs, RNNs, LSTMs, Transformers
✅ Advanced training: mixed precision, gradient accumulation
✅ Generative models: GANs, VAEs, Diffusion
✅ Reinforcement Learning fundamentals

Week 3: NLP, CV & Generative AI
✅ Embeddings, BERT fine-tuning, GPT APIs
✅ Object detection (YOLO), segmentation
✅ LLM prompting strategies
✅ RAG architecture and implementation
✅ AI agents with tool use
✅ Multimodal AI (CLIP, GPT-4V)

Week 4: Production & MLOps
✅ LLM fine-tuning with LoRA/PEFT
✅ Vector databases and semantic search
✅ MLflow, experiment tracking, model registry
✅ FastAPI model serving
✅ AI ethics, bias, hallucination mitigation
✅ AI system design patterns
✅ Performance optimization and scaling
✅ Capstone: End-to-end AI application
```

#### Your Personalized Next Steps

```python
next_steps = {
    "If you want to go deeper on LLMs": [
        "Hugging Face NLP Course (free)",
        "Build and fine-tune LLaMA on custom data",
        "Study RLHF and InstructGPT paper",
        "Implement an AI agent framework from scratch"
    ],
    "If you want ML Engineering / MLOps": [
        "Full Stack Deep Learning course",
        "Made With ML by Goku Mohandas",
        "Get hands-on with Kubernetes + KServe",
        "Study data versioning (DVC) and feature stores (Feast)"
    ],
    "If you want AI Research": [
        "Read 1 paper/day from arXiv cs.LG or cs.CL",
        "Reproduce 3 key papers (Transformer, ResNet, DDPM)",
        "Contribute to open-source: Hugging Face, PyTorch",
        "Apply to NeurIPS / ICML workshop papers"
    ],
    "If you want Generative AI Products": [
        "Build with LangChain and LlamaIndex",
        "Study prompt engineering deeply",
        "Explore function calling and structured outputs",
        "Ship a production AI SaaS side project"
    ]
}
```

---

## 📊 Complete 30-Day Summary

| Week | Days | Theme | Key Topics |
|---|---|---|---|
| **Week 1** | 1–7 | AI Foundations | AI landscape, Python stack, regression, classification, clustering, ensembles |
| **Week 2** | 8–14 | Deep Learning | Neural networks, CNNs, RNNs, Transformers, GANs, RL |
| **Week 3** | 15–21 | NLP, CV & GenAI | BERT, YOLO, LLM APIs, RAG, AI agents, multimodal AI |
| **Week 4** | 22–30 | Production AI | LoRA, vector DBs, MLOps, serving, ethics, system design, capstone |

---

## ⚡ Must-Read Papers

| Paper | Year | Why It Matters |
|---|---|---|
| Attention Is All You Need | 2017 | Invented the Transformer |
| BERT | 2018 | Bidirectional language understanding |
| GPT-3 | 2020 | Few-shot learning at massive scale |
| ResNet | 2015 | Skip connections, trained 152-layer networks |
| DDPM | 2020 | Foundation of diffusion models (Stable Diffusion) |
| LoRA | 2021 | Efficient fine-tuning with <1% parameters |
| CLIP | 2021 | Vision-language alignment |
| InstructGPT | 2022 | RLHF — how ChatGPT was aligned |
| LLaMA 2 | 2023 | Open-source large language model |
| RAG | 2020 | Retrieval-augmented generation |

---

## 📖 Best Resources

| Category | Resource | Why |
|---|---|---|
| Courses | [fast.ai Practical DL](https://course.fast.ai/) | Best top-down DL course |
| Courses | [DeepLearning.AI](https://www.deeplearning.ai/) | Andrew Ng's structured path |
| Courses | [Hugging Face Course](https://huggingface.co/learn) | Free NLP + LLMs |
| Books | *Deep Learning* — Goodfellow et al. | Authoritative DL theory |
| Books | *Hands-On ML* — Aurélien Géron | Best scikit-learn + DL book |
| Books | *Designing ML Systems* — Chip Huyen | Production ML bible |
| Papers | [Papers With Code](https://paperswithcode.com/) | State-of-the-art results |
| Practice | [Kaggle](https://www.kaggle.com/competitions) | Real ML competitions |
| Practice | [LeetCode (ML section)](https://leetcode.com/) | ML interview prep |
| Community | [r/MachineLearning](https://reddit.com/r/MachineLearning) | Research discussions |

---

## ⚠️ Top 10 AI Engineering Pitfalls

1. **Training/serving skew** — features differ between training and production; use a feature store
2. **Data leakage** — future info leaks into training; causes inflated metrics that collapse in prod
3. **Ignoring baselines** — always start with a simple model; complexity should earn its cost
4. **Not monitoring in production** — models degrade silently; always monitor data + model drift
5. **Prompt injection** — LLM apps are vulnerable; validate and sanitize all user inputs
6. **LLM hallucinations** — never trust raw LLM output for factual claims; add RAG or grounding
7. **Token cost blindness** — LLM APIs are expensive at scale; cache aggressively, track usage
8. **Over-engineering** — an XGBoost model often beats a deep network on tabular data
9. **No evaluation harness** — define offline metrics and test sets before writing model code
10. **Class imbalance blindness** — using accuracy on imbalanced datasets hides failures

---

*From numpy arrays to AI agents — the full journey in 30 days. Keep building! 🚀*
