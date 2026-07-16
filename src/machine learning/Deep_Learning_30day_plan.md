# 🧠 Deep Learning — 30-Day Learning Plan
### *A Senior Software Engineer's Structured Path to Deep Learning Mastery*

> **Prerequisites:** Python proficiency, NumPy/pandas basics, linear algebra fundamentals, basic calculus.
> **Goal:** Go from ML foundations to deploying production-grade deep learning models in 30 days.
> **Time Commitment:** ~2–3 hours/day.

---

## 📦 Environment Setup (Before Day 1)

```bash
# Create isolated environment
python -m venv dl_env
source dl_env/bin/activate       # Windows: dl_env\Scripts\activate

# Install core stack
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
pip install tensorflow keras
pip install numpy pandas matplotlib seaborn scikit-learn
pip install jupyter notebook ipywidgets
pip install tensorboard wandb

# Verify GPU
python -c "import torch; print(torch.cuda.is_available())"
```

---

## 🗓️ Week 1 — Mathematical Foundations & ML Review (Days 1–7)

---

### Day 1 — Linear Algebra for Deep Learning

#### 🎯 Objectives
- Understand vectors, matrices, and tensors
- Master operations that power neural networks

#### 📚 Core Concepts

```python
import numpy as np

# Vectors
v = np.array([1, 2, 3])
print(v.shape)          # (3,)
print(np.dot(v, v))     # dot product = 14

# Matrices
A = np.array([[1, 2], [3, 4]])
B = np.array([[5, 6], [7, 8]])
print(A @ B)            # Matrix multiplication
print(A.T)              # Transpose

# Eigenvalues (key for PCA, optimization)
eigenvalues, eigenvectors = np.linalg.eig(A)

# Broadcasting — critical in DL
X = np.random.randn(100, 784)   # 100 samples, 784 features
W = np.random.randn(784, 256)   # Weight matrix
Z = X @ W                        # Shape: (100, 256)
```

#### Key Concepts to Master
- **Dot product** — similarity, projections
- **Matrix multiplication** — how layers transform inputs
- **Transpose** — weight sharing, attention
- **Norms** — L1, L2 regularization
- **SVD / Eigendecomposition** — PCA, compression

#### 💡 Senior Tips
- Think in shapes. Before writing any layer, know `input_shape → weight_shape → output_shape`.
- Broadcasting is everywhere in DL. Understand it deeply to avoid silent bugs.

---

### Day 2 — Calculus & Optimization Fundamentals

#### 🎯 Objectives
- Understand gradients, partial derivatives, and the chain rule
- See how backpropagation works at the math level

#### 📚 Core Concepts

```python
import torch

# Automatic differentiation with PyTorch
x = torch.tensor(3.0, requires_grad=True)
y = x ** 2 + 2 * x + 1   # y = x^2 + 2x + 1

y.backward()              # Compute dy/dx
print(x.grad)             # dy/dx at x=3: 2*3+2 = 8.0

# Multivariable gradient
a = torch.tensor([1.0, 2.0, 3.0], requires_grad=True)
loss = (a ** 2).sum()     # loss = a1^2 + a2^2 + a3^2
loss.backward()
print(a.grad)             # [2, 4, 6]
```

#### Chain Rule — The Heart of Backprop
```
Forward:  x -> [Layer1] -> h -> [Layer2] -> y -> [Loss] -> L
Backward: dL/dx = dL/dy * dy/dh * dh/dx
```

#### Gradient Descent Variants
| Variant | Update Rule | Best For |
|---|---|---|
| Batch GD | Full dataset per step | Small datasets |
| SGD | 1 sample per step | Online learning |
| Mini-batch | N samples per step | Standard practice |

#### 💡 Senior Tips
- Gradient descent is the engine of all DL. No magic — just compute the slope, step down.
- `requires_grad=True` is your debugging friend; trace gradients through any computation.

---

### Day 3 — Probability & Information Theory

#### 🎯 Objectives
- Understand probability distributions used in DL
- Know cross-entropy loss, KL-divergence, and likelihood

#### 📚 Core Concepts

```python
import numpy as np

# Softmax — converts logits to probabilities
def softmax(z):
    exp_z = np.exp(z - np.max(z))  # Numerical stability trick
    return exp_z / exp_z.sum()

logits = np.array([2.0, 1.0, 0.1])
probs = softmax(logits)  # [0.659, 0.242, 0.099]

# Cross-Entropy Loss
def cross_entropy(y_true, y_pred):
    return -np.sum(y_true * np.log(y_pred + 1e-9))

y_true = np.array([1, 0, 0])    # One-hot: class 0
loss = cross_entropy(y_true, probs)

# KL Divergence — measures distribution distance
def kl_divergence(p, q):
    return np.sum(p * np.log(p / (q + 1e-9)))
```

#### Key Distributions in DL
| Distribution | Use Case |
|---|---|
| Gaussian (Normal) | Weight initialization, VAE latent space |
| Bernoulli | Binary classification output |
| Categorical (Softmax) | Multi-class classification |
| Uniform | Random initialization bounds |

#### 💡 Senior Tips
- Cross-entropy is negative log-likelihood for categorical distributions. Know this derivation.
- The `1e-9` epsilon in log prevents `log(0) = -inf`. Always add it.

---

### Day 4 — Classic ML Review (Essential Context)

#### 🎯 Objectives
- Review key ML algorithms that underpin DL design decisions
- Understand when NOT to use deep learning

#### 📚 Quick ML Refresher

```python
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split

# Standard ML pipeline
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
scaler = StandardScaler()
X_train = scaler.fit_transform(X_train)
X_test = scaler.transform(X_test)

models = {
    'Logistic Regression': LogisticRegression(),
    'Random Forest': RandomForestClassifier(n_estimators=100),
    'Gradient Boosting': GradientBoostingClassifier()
}

for name, model in models.items():
    model.fit(X_train, y_train)
    score = accuracy_score(y_test, model.predict(X_test))
    print(f"{name}: {score:.4f}")
```

#### When to Use Deep Learning vs. Classic ML
| Scenario | Use DL | Use Classic ML |
|---|---|---|
| Tabular data (<100K rows) | No | Yes (XGBoost) |
| Images, audio, text | Yes | No |
| Interpretability required | No | Yes |
| Limited compute | No | Yes |
| Large unlabeled datasets | Yes (self-supervised) | Rarely |

#### 💡 Senior Tips
- DL is not always better. XGBoost still wins on structured/tabular data.
- Know your baselines. A logistic regression baseline should always come first.

---

### Day 5 — Introduction to Neural Networks

#### 🎯 Objectives
- Build a neural network from scratch with NumPy
- Understand forward pass, loss computation, and backpropagation

#### 📚 Neural Network from Scratch

```python
import numpy as np

class NeuralNetwork:
    def __init__(self, input_size, hidden_size, output_size):
        # Xavier initialization
        self.W1 = np.random.randn(input_size, hidden_size) * np.sqrt(2 / input_size)
        self.b1 = np.zeros((1, hidden_size))
        self.W2 = np.random.randn(hidden_size, output_size) * np.sqrt(2 / hidden_size)
        self.b2 = np.zeros((1, output_size))

    def relu(self, z):
        return np.maximum(0, z)

    def relu_grad(self, z):
        return (z > 0).astype(float)

    def softmax(self, z):
        exp_z = np.exp(z - np.max(z, axis=1, keepdims=True))
        return exp_z / exp_z.sum(axis=1, keepdims=True)

    def forward(self, X):
        self.z1 = X @ self.W1 + self.b1
        self.a1 = self.relu(self.z1)
        self.z2 = self.a1 @ self.W2 + self.b2
        self.a2 = self.softmax(self.z2)
        return self.a2

    def backward(self, X, y, lr=0.01):
        m = X.shape[0]
        dz2 = self.a2 - y                           # Softmax + CE gradient
        dW2 = (self.a1.T @ dz2) / m
        db2 = dz2.mean(axis=0, keepdims=True)

        da1 = dz2 @ self.W2.T
        dz1 = da1 * self.relu_grad(self.z1)
        dW1 = (X.T @ dz1) / m
        db1 = dz1.mean(axis=0, keepdims=True)

        # Parameter update
        self.W2 -= lr * dW2
        self.b2 -= lr * db2
        self.W1 -= lr * dW1
        self.b1 -= lr * db1
```

#### 💡 Senior Tips
- Building from scratch ONCE is mandatory. You'll never debug a framework confidently otherwise.
- Xavier/He initialization matters enormously — random init causes vanishing/exploding gradients.

---

### Day 6 — PyTorch Fundamentals

#### 🎯 Objectives
- Master PyTorch tensors and autograd
- Build and train your first neural network with PyTorch

#### 📚 Core PyTorch

```python
import torch
import torch.nn as nn
import torch.optim as optim

# Tensors
x = torch.randn(3, 4)
x = x.cuda()                    # Move to GPU
x = x.to('cuda')                # Alternative
print(x.device, x.shape, x.dtype)

# Build a model with nn.Sequential
model = nn.Sequential(
    nn.Linear(784, 256),
    nn.ReLU(),
    nn.Dropout(0.3),
    nn.Linear(256, 128),
    nn.ReLU(),
    nn.Linear(128, 10)
)

# Or using nn.Module (preferred for complex models)
class MLP(nn.Module):
    def __init__(self):
        super().__init__()
        self.layers = nn.Sequential(
            nn.Linear(784, 256),
            nn.BatchNorm1d(256),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(256, 10)
        )

    def forward(self, x):
        return self.layers(x)

# Training loop
model = MLP()
optimizer = optim.Adam(model.parameters(), lr=1e-3)
criterion = nn.CrossEntropyLoss()

for epoch in range(10):
    model.train()
    optimizer.zero_grad()
    outputs = model(X_batch)
    loss = criterion(outputs, y_batch)
    loss.backward()
    optimizer.step()
```

#### 💡 Senior Tips
- `optimizer.zero_grad()` before `loss.backward()` is crucial — gradients accumulate by default.
- Use `model.train()` and `model.eval()` — they control Dropout and BatchNorm behavior.

---

### Day 7 — Data Pipelines & Week 1 Review

#### 🎯 Objectives
- Build efficient data loading pipelines with `DataLoader`
- Review and consolidate Week 1

#### 📚 PyTorch Data Pipeline

```python
from torch.utils.data import Dataset, DataLoader
from torchvision import datasets, transforms

# Custom Dataset
class CustomDataset(Dataset):
    def __init__(self, X, y, transform=None):
        self.X = torch.FloatTensor(X)
        self.y = torch.LongTensor(y)
        self.transform = transform

    def __len__(self):
        return len(self.X)

    def __getitem__(self, idx):
        sample = self.X[idx]
        if self.transform:
            sample = self.transform(sample)
        return sample, self.y[idx]

# DataLoader with augmentation
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.5,), (0.5,)),
    transforms.RandomHorizontalFlip(),
    transforms.RandomCrop(32, padding=4)
])

train_dataset = datasets.CIFAR10(root='./data', train=True, transform=transform, download=True)
train_loader = DataLoader(train_dataset, batch_size=64, shuffle=True, num_workers=4, pin_memory=True)
```

#### Week 1 Checklist
- [ ] Linear algebra: matrix ops, broadcasting, norms
- [ ] Calculus: gradients, chain rule, backprop derivation
- [ ] Probability: cross-entropy, softmax, distributions
- [ ] Built a neural network from scratch with NumPy
- [ ] PyTorch: tensors, autograd, nn.Module, training loop
- [ ] Data pipelines: Dataset, DataLoader, transforms

---

## 🗓️ Week 2 — Core Architectures (Days 8–14)

---

### Day 8 — Activation Functions, Loss Functions & Optimizers

#### 🎯 Objectives
- Know when to use which activation function
- Master modern optimizers beyond basic SGD

#### 📚 Activation Functions

```python
import torch
import torch.nn.functional as F

# Comparison
x = torch.linspace(-3, 3, 100)

sigmoid    = torch.sigmoid(x)        # Output: (0, 1) — binary classification output
tanh       = torch.tanh(x)           # Output: (-1, 1) — older RNNs
relu       = F.relu(x)               # Output: [0, inf) — hidden layers
leaky_relu = F.leaky_relu(x, 0.01)   # Output: (-inf, inf) — avoids dying ReLU
gelu       = F.gelu(x)               # Output: smooth — Transformers (BERT, GPT)
swish      = x * torch.sigmoid(x)    # Output: smooth — EfficientNet

# Dead ReLU problem — use LeakyReLU or GELU in deep nets
```

#### Optimizers Deep Dive

```python
# SGD with momentum
optimizer = optim.SGD(model.parameters(), lr=0.01, momentum=0.9, weight_decay=1e-4)

# Adam — adaptive learning rates (default choice)
optimizer = optim.Adam(model.parameters(), lr=1e-3, betas=(0.9, 0.999), eps=1e-8)

# AdamW — Adam with proper weight decay (best for Transformers)
optimizer = optim.AdamW(model.parameters(), lr=1e-3, weight_decay=0.01)

# Learning rate scheduler
scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=100)
scheduler = optim.lr_scheduler.OneCycleLR(optimizer, max_lr=0.01, steps_per_epoch=len(train_loader), epochs=10)
```

#### Optimizer Comparison
| Optimizer | Adaptive LR | Weight Decay | Best For |
|---|---|---|---|
| SGD + Momentum | No | L2 | CNNs, ResNets |
| Adam | Yes | Coupled | General purpose |
| AdamW | Yes | Decoupled | Transformers, LLMs |
| RMSProp | Yes | No | RNNs |

---

### Day 9 — Regularization Techniques

#### 🎯 Objectives
- Combat overfitting with a toolkit of regularization strategies
- Understand when and how to apply each

#### 📚 Regularization Methods

```python
import torch.nn as nn

# 1. Dropout — randomly zero neurons during training
nn.Dropout(p=0.5)           # 50% chance each neuron is zeroed
nn.Dropout2d(p=0.2)         # For conv layers — drops entire channels

# 2. Batch Normalization — normalize layer inputs
nn.BatchNorm1d(256)         # For MLP layers
nn.BatchNorm2d(64)          # For CNN feature maps

# 3. Layer Normalization — normalize across features (Transformers)
nn.LayerNorm(512)

# 4. L2 Regularization via weight decay
optimizer = optim.Adam(model.parameters(), lr=1e-3, weight_decay=1e-4)

# 5. Early stopping (manual implementation)
class EarlyStopping:
    def __init__(self, patience=5):
        self.patience = patience
        self.counter = 0
        self.best_loss = float('inf')

    def step(self, val_loss):
        if val_loss < self.best_loss:
            self.best_loss = val_loss
            self.counter = 0
        else:
            self.counter += 1
        return self.counter >= self.patience    # True = stop training

# 6. Data Augmentation (best regularizer for images)
transforms.Compose([
    transforms.RandomHorizontalFlip(),
    transforms.RandomRotation(10),
    transforms.ColorJitter(brightness=0.2, contrast=0.2),
    transforms.RandomErasing(p=0.3)
])
```

#### 💡 Senior Tips
- BatchNorm reduces need for Dropout in CNNs. Don't stack both aggressively.
- The best regularizer is more data. Always exhaust augmentation before adding L2.

---

### Day 10 — Convolutional Neural Networks (CNNs)

#### 🎯 Objectives
- Understand convolution, pooling, and receptive fields
- Build and train a CNN on image data

#### 📚 CNN Building Blocks

```python
import torch.nn as nn

# Convolution anatomy
# nn.Conv2d(in_channels, out_channels, kernel_size, stride, padding)
# Output size: (W - K + 2P) / S + 1

class ConvBlock(nn.Module):
    def __init__(self, in_ch, out_ch):
        super().__init__()
        self.block = nn.Sequential(
            nn.Conv2d(in_ch, out_ch, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(out_ch),
            nn.ReLU(inplace=True)
        )
    def forward(self, x):
        return self.block(x)

class SimpleCNN(nn.Module):
    def __init__(self, num_classes=10):
        super().__init__()
        self.features = nn.Sequential(
            ConvBlock(3, 32),           # 32x32x3 -> 32x32x32
            nn.MaxPool2d(2),            # 32x32x32 -> 16x16x32
            ConvBlock(32, 64),          # 16x16x32 -> 16x16x64
            nn.MaxPool2d(2),            # 16x16x64 -> 8x8x64
            ConvBlock(64, 128),
            nn.AdaptiveAvgPool2d(1)     # -> 1x1x128 (global avg pool)
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Linear(128, 256),
            nn.ReLU(),
            nn.Dropout(0.5),
            nn.Linear(256, num_classes)
        )

    def forward(self, x):
        return self.classifier(self.features(x))
```

#### CNN Architecture Evolution
| Model | Year | Innovation |
|---|---|---|
| LeNet-5 | 1998 | First CNN |
| AlexNet | 2012 | Deep CNN, ReLU, Dropout |
| VGGNet | 2014 | Very deep, 3x3 convs only |
| GoogLeNet | 2014 | Inception modules |
| ResNet | 2015 | Residual connections |
| EfficientNet | 2019 | Neural Architecture Search |

---

### Day 11 — Transfer Learning & Fine-Tuning

#### 🎯 Objectives
- Use pretrained models for custom tasks
- Understand when to freeze vs. fine-tune layers

#### 📚 Transfer Learning with PyTorch

```python
import torchvision.models as models

# Load pretrained ResNet50
model = models.resnet50(pretrained=True)

# Strategy 1: Feature Extraction — freeze all except classifier
for param in model.parameters():
    param.requires_grad = False

# Replace classifier head
model.fc = nn.Sequential(
    nn.Linear(model.fc.in_features, 256),
    nn.ReLU(),
    nn.Dropout(0.3),
    nn.Linear(256, num_classes)
)

# Only new head parameters are trainable
optimizer = optim.Adam(model.fc.parameters(), lr=1e-3)

# Strategy 2: Fine-Tuning — unfreeze all, use tiny LR
for param in model.parameters():
    param.requires_grad = True

# Differential learning rates — lower LR for pretrained layers
optimizer = optim.Adam([
    {'params': model.layer1.parameters(), 'lr': 1e-5},
    {'params': model.layer2.parameters(), 'lr': 1e-5},
    {'params': model.layer3.parameters(), 'lr': 1e-4},
    {'params': model.layer4.parameters(), 'lr': 1e-4},
    {'params': model.fc.parameters(),     'lr': 1e-3},
])
```

#### Transfer Learning Decision Guide
| Data | Similarity to Source | Strategy |
|---|---|---|
| Small | High similarity | Feature extract only |
| Small | Low similarity | Feature extract + retrain top layers |
| Large | High similarity | Fine-tune all layers |
| Large | Low similarity | Train from scratch |

---

### Day 12 — Recurrent Neural Networks (RNNs & LSTMs)

#### 🎯 Objectives
- Understand sequential modeling with RNNs
- Know why LSTMs and GRUs were invented

#### 📚 Sequence Modeling

```python
import torch.nn as nn

# Vanilla RNN — suffers vanishing gradients
rnn = nn.RNN(input_size=100, hidden_size=256, num_layers=2,
             batch_first=True, dropout=0.3)

# LSTM — Long Short-Term Memory (solves vanishing gradient)
class LSTMClassifier(nn.Module):
    def __init__(self, vocab_size, embed_dim, hidden_size, num_classes):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=0)
        self.lstm = nn.LSTM(embed_dim, hidden_size, num_layers=2,
                            batch_first=True, dropout=0.3, bidirectional=True)
        self.classifier = nn.Linear(hidden_size * 2, num_classes)  # *2 for bidirectional

    def forward(self, x):
        embedded = self.embedding(x)               # (batch, seq, embed)
        out, (hn, cn) = self.lstm(embedded)
        # Use last hidden state from both directions
        final_hidden = torch.cat([hn[-2], hn[-1]], dim=1)
        return self.classifier(final_hidden)

# GRU — Gated Recurrent Unit (simpler than LSTM, often similar performance)
gru = nn.GRU(input_size=100, hidden_size=256, num_layers=2,
             batch_first=True, bidirectional=True)
```

#### LSTM Gate Summary
| Gate | Role |
|---|---|
| Forget gate | What to erase from memory |
| Input gate | What new info to store |
| Output gate | What to read from memory |
| Cell state | Long-term memory highway |

---

### Day 13 — Attention Mechanism & Transformers Intro

#### 🎯 Objectives
- Understand self-attention and multi-head attention
- See how Transformers replaced RNNs

#### 📚 Attention Mechanism

```python
import torch
import torch.nn.functional as F
import math

def scaled_dot_product_attention(Q, K, V, mask=None):
    """
    Q: (batch, heads, seq, d_k)
    K: (batch, heads, seq, d_k)
    V: (batch, heads, seq, d_v)
    """
    d_k = Q.shape[-1]
    scores = (Q @ K.transpose(-2, -1)) / math.sqrt(d_k)    # (batch, heads, seq, seq)
    if mask is not None:
        scores = scores.masked_fill(mask == 0, float('-inf'))
    weights = F.softmax(scores, dim=-1)                      # Attention weights
    return weights @ V                                        # (batch, heads, seq, d_v)

class MultiHeadAttention(nn.Module):
    def __init__(self, d_model, num_heads):
        super().__init__()
        assert d_model % num_heads == 0
        self.d_k = d_model // num_heads
        self.num_heads = num_heads
        self.W_q = nn.Linear(d_model, d_model)
        self.W_k = nn.Linear(d_model, d_model)
        self.W_v = nn.Linear(d_model, d_model)
        self.W_o = nn.Linear(d_model, d_model)

    def forward(self, Q, K, V, mask=None):
        batch = Q.shape[0]
        Q = self.W_q(Q).view(batch, -1, self.num_heads, self.d_k).transpose(1, 2)
        K = self.W_k(K).view(batch, -1, self.num_heads, self.d_k).transpose(1, 2)
        V = self.W_v(V).view(batch, -1, self.num_heads, self.d_k).transpose(1, 2)
        attn = scaled_dot_product_attention(Q, K, V, mask)
        attn = attn.transpose(1, 2).contiguous().view(batch, -1, self.num_heads * self.d_k)
        return self.W_o(attn)
```

#### 💡 Senior Tips
- Attention is O(n²) in sequence length. This is why LLMs have context window limits.
- Multi-head attention lets the model attend to different positions for different reasons simultaneously.

---

### Day 14 — Transformer Architecture (Full) & Week 2 Review

#### 🎯 Objectives
- Build a complete Transformer encoder
- Use `torch.nn.Transformer` for a real task

#### 📚 Transformer Encoder Block

```python
class TransformerBlock(nn.Module):
    def __init__(self, d_model, num_heads, ff_dim, dropout=0.1):
        super().__init__()
        self.attention = nn.MultiheadAttention(d_model, num_heads, dropout=dropout, batch_first=True)
        self.ff = nn.Sequential(
            nn.Linear(d_model, ff_dim),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(ff_dim, d_model)
        )
        self.norm1 = nn.LayerNorm(d_model)
        self.norm2 = nn.LayerNorm(d_model)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x, mask=None):
        # Pre-norm variant (more stable training)
        attn_out, _ = self.attention(self.norm1(x), self.norm1(x), self.norm1(x), attn_mask=mask)
        x = x + self.dropout(attn_out)
        x = x + self.dropout(self.ff(self.norm2(x)))
        return x

class TransformerEncoder(nn.Module):
    def __init__(self, vocab_size, d_model, num_heads, num_layers, ff_dim, max_seq_len, num_classes):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, d_model)
        self.pos_embedding = nn.Embedding(max_seq_len, d_model)
        self.layers = nn.ModuleList([TransformerBlock(d_model, num_heads, ff_dim) for _ in range(num_layers)])
        self.classifier = nn.Linear(d_model, num_classes)

    def forward(self, x):
        positions = torch.arange(x.shape[1], device=x.device)
        x = self.embedding(x) + self.pos_embedding(positions)
        for layer in self.layers:
            x = layer(x)
        return self.classifier(x.mean(dim=1))  # Mean pooling
```

#### Week 2 Checklist
- [ ] Activation functions and their trade-offs
- [ ] Modern optimizers: Adam, AdamW, LR schedulers
- [ ] Regularization: Dropout, BatchNorm, weight decay
- [ ] CNNs: convolution, pooling, architecture
- [ ] Transfer learning and fine-tuning strategies
- [ ] RNNs, LSTMs, and GRUs
- [ ] Attention mechanism and Transformer architecture

---

## 🗓️ Week 3 — Modern Architectures & NLP (Days 15–21)

---

### Day 15 — ResNet, EfficientNet & Modern CNN Designs

```python
# Residual block — core of ResNet
class ResidualBlock(nn.Module):
    def __init__(self, channels):
        super().__init__()
        self.block = nn.Sequential(
            nn.Conv2d(channels, channels, 3, padding=1, bias=False),
            nn.BatchNorm2d(channels),
            nn.ReLU(inplace=True),
            nn.Conv2d(channels, channels, 3, padding=1, bias=False),
            nn.BatchNorm2d(channels)
        )
        self.relu = nn.ReLU(inplace=True)

    def forward(self, x):
        return self.relu(x + self.block(x))   # Skip connection
```

**Key insight:** The skip connection solves vanishing gradients — gradients flow directly through identity shortcuts.

---

### Day 16 — BERT & Pre-trained Language Models

```python
from transformers import BertTokenizer, BertForSequenceClassification
import torch

tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
model = BertForSequenceClassification.from_pretrained('bert-base-uncased', num_labels=2)

# Tokenize
text = "The movie was absolutely fantastic!"
inputs = tokenizer(text, return_tensors='pt', max_length=128,
                   truncation=True, padding='max_length')

# Fine-tune
outputs = model(**inputs, labels=torch.tensor([1]))
loss = outputs.loss
loss.backward()
```

---

### Day 17 — GPT & Autoregressive Language Models

#### 🎯 Objectives
- Understand causal (decoder-only) language modeling
- See how GPT generates text token by token

```python
from transformers import GPT2LMHeadModel, GPT2Tokenizer

tokenizer = GPT2Tokenizer.from_pretrained('gpt2')
model = GPT2LMHeadModel.from_pretrained('gpt2')

# Text generation
input_ids = tokenizer.encode("Deep learning is", return_tensors='pt')
output = model.generate(
    input_ids,
    max_length=100,
    num_beams=5,                    # Beam search
    temperature=0.8,                # Sampling temperature
    top_p=0.9,                      # Nucleus sampling
    repetition_penalty=1.3,
    do_sample=True
)
print(tokenizer.decode(output[0]))
```

#### BERT vs. GPT
| | BERT (Encoder) | GPT (Decoder) |
|---|---|---|
| Training | Masked LM | Next token prediction |
| Attention | Bidirectional | Causal (left-to-right) |
| Best for | Classification, NER, QA | Text generation |
| Scale | Base: 110M | GPT-4: ~1T params |

---

### Day 18 — Vision Transformers (ViT) & CLIP

```python
from transformers import ViTForImageClassification, ViTFeatureExtractor

# ViT patches an image into tokens and applies Transformer
extractor = ViTFeatureExtractor.from_pretrained('google/vit-base-patch16-224')
model = ViTForImageClassification.from_pretrained('google/vit-base-patch16-224')

# CLIP — connects vision and language
from transformers import CLIPModel, CLIPProcessor

clip_model = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
# Zero-shot image classification with text prompts
```

---

### Day 19 — Generative Models: VAEs & GANs

```python
# Variational Autoencoder
class VAE(nn.Module):
    def __init__(self, input_dim, latent_dim):
        super().__init__()
        self.encoder = nn.Sequential(nn.Linear(input_dim, 512), nn.ReLU())
        self.mu_head = nn.Linear(512, latent_dim)
        self.logvar_head = nn.Linear(512, latent_dim)
        self.decoder = nn.Sequential(
            nn.Linear(latent_dim, 512),
            nn.ReLU(),
            nn.Linear(512, input_dim),
            nn.Sigmoid()
        )

    def reparameterize(self, mu, logvar):
        std = torch.exp(0.5 * logvar)
        eps = torch.randn_like(std)
        return mu + eps * std                # Reparameterization trick

    def forward(self, x):
        h = self.encoder(x)
        mu, logvar = self.mu_head(h), self.logvar_head(h)
        z = self.reparameterize(mu, logvar)
        return self.decoder(z), mu, logvar

# VAE Loss = Reconstruction Loss + KL Divergence
def vae_loss(recon_x, x, mu, logvar):
    recon_loss = F.binary_cross_entropy(recon_x, x, reduction='sum')
    kld = -0.5 * torch.sum(1 + logvar - mu.pow(2) - logvar.exp())
    return recon_loss + kld
```

---

### Day 20 — Diffusion Models

#### 🎯 Objectives
- Understand the math behind diffusion models (DDPM)
- See how Stable Diffusion and DALL-E work at a high level

#### Core Concept
```python
# Forward diffusion: gradually add Gaussian noise
# q(x_t | x_{t-1}) = N(sqrt(1-beta_t)*x_{t-1}, beta_t * I)

# Reverse diffusion: learn to denoise
# p_theta(x_{t-1} | x_t) = N(mu_theta(x_t, t), sigma_t * I)

# The model (U-Net) predicts the noise at each step
# Then we subtract that noise to recover the original signal

from diffusers import StableDiffusionPipeline

pipe = StableDiffusionPipeline.from_pretrained(
    "runwayml/stable-diffusion-v1-5",
    torch_dtype=torch.float16
).to("cuda")

image = pipe("A futuristic city at sunset, digital art").images[0]
image.save("generated.png")
```

#### Architecture of Diffusion Models
```
Prompt --> Text Encoder (CLIP) --> Conditioning
Latent Noise --> U-Net (with cross-attention) --> Denoised Latent --> VAE Decoder --> Image
```

---

### Day 21 — Reinforcement Learning Basics & Week 3 Review

```python
import gym

# Q-Learning with Neural Network (DQN)
class DQN(nn.Module):
    def __init__(self, state_dim, action_dim):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(state_dim, 128), nn.ReLU(),
            nn.Linear(128, 128),       nn.ReLU(),
            nn.Linear(128, action_dim)
        )
    def forward(self, x):
        return self.net(x)

# Bellman equation: Q(s,a) = r + gamma * max_a' Q(s', a')
```

#### Week 3 Checklist
- [ ] ResNet residual connections
- [ ] BERT fine-tuning for NLP tasks
- [ ] GPT text generation strategies
- [ ] Vision Transformers and multimodal models (CLIP)
- [ ] VAE generative modeling
- [ ] Diffusion models conceptually
- [ ] RL basics: DQN, Bellman equation

---

## 🗓️ Week 4 — Production, MLOps & Advanced Topics (Days 22–30)

---

### Day 22 — Experiment Tracking & Hyperparameter Tuning

```python
import wandb

# Weights & Biases experiment tracking
wandb.init(project="my-dl-project", config={
    "learning_rate": 1e-3,
    "batch_size": 64,
    "epochs": 50,
    "architecture": "ResNet50"
})

for epoch in range(config.epochs):
    train_loss = train_one_epoch(model, train_loader)
    val_acc = evaluate(model, val_loader)

    wandb.log({
        "epoch": epoch,
        "train_loss": train_loss,
        "val_accuracy": val_acc,
        "lr": optimizer.param_groups[0]['lr']
    })

    # Save best model
    if val_acc > best_acc:
        wandb.run.summary["best_accuracy"] = val_acc
        torch.save(model.state_dict(), "best_model.pth")

wandb.finish()

# Optuna for hyperparameter search
import optuna

def objective(trial):
    lr = trial.suggest_float("lr", 1e-5, 1e-1, log=True)
    dropout = trial.suggest_float("dropout", 0.1, 0.5)
    hidden = trial.suggest_categorical("hidden_size", [128, 256, 512])
    model = MLP(hidden_size=hidden, dropout=dropout)
    optimizer = optim.Adam(model.parameters(), lr=lr)
    return train_and_evaluate(model, optimizer)

study = optuna.create_study(direction="maximize")
study.optimize(objective, n_trials=50)
print(study.best_params)
```

---

### Day 23 — Model Optimization: Quantization & Pruning

```python
import torch.quantization

# Post-training quantization (INT8 for inference speed)
model.eval()
model_quantized = torch.quantization.quantize_dynamic(
    model,
    {nn.Linear, nn.LSTM},
    dtype=torch.qint8
)

# Compare sizes
original_size = sum(p.numel() * p.element_size() for p in model.parameters()) / 1e6
quantized_size = sum(p.numel() * p.element_size() for p in model_quantized.parameters()) / 1e6
print(f"Original: {original_size:.1f}MB | Quantized: {quantized_size:.1f}MB")

# Pruning — remove unimportant weights
import torch.nn.utils.prune as prune

prune.l1_unstructured(model.fc, name='weight', amount=0.3)  # Prune 30% of weights
prune.remove(model.fc, 'weight')  # Make pruning permanent
```

#### Model Compression Techniques
| Technique | Speedup | Accuracy Loss | Use Case |
|---|---|---|---|
| INT8 Quantization | 2-4x | <1% | Edge inference |
| FP16 Mixed Precision | 1.5-2x | ~0% | GPU training |
| Pruning (30%) | ~1.3x | <2% | Mobile deployment |
| Knowledge Distillation | 5-10x | 2-5% | Small model from large |

---

### Day 24 — Deployment with TorchScript & ONNX

```python
# TorchScript — export model for C++ inference
model.eval()
scripted_model = torch.jit.script(model)
scripted_model.save("model_scripted.pt")

# Load in production
loaded = torch.jit.load("model_scripted.pt")
output = loaded(input_tensor)

# ONNX — cross-platform export
dummy_input = torch.randn(1, 3, 224, 224)
torch.onnx.export(
    model, dummy_input, "model.onnx",
    input_names=['input'],
    output_names=['output'],
    dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}},
    opset_version=17
)

# Verify ONNX
import onnx, onnxruntime as ort
onnx.checker.check_model("model.onnx")
session = ort.InferenceSession("model.onnx")
outputs = session.run(None, {"input": dummy_input.numpy()})
```

---

### Day 25 — Serving with FastAPI & Docker

```python
# FastAPI model server
from fastapi import FastAPI, File, UploadFile
from PIL import Image
import io

app = FastAPI(title="DL Model API")

@app.on_event("startup")
async def load_model():
    global model, transform
    model = torch.jit.load("model_scripted.pt").eval()
    transform = transforms.Compose([
        transforms.Resize(224),
        transforms.CenterCrop(224),
        transforms.ToTensor(),
        transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
    ])

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    img = Image.open(io.BytesIO(await file.read())).convert("RGB")
    tensor = transform(img).unsqueeze(0)
    with torch.no_grad():
        logits = model(tensor)
        probs = torch.softmax(logits, dim=1)
        pred = probs.argmax().item()
        confidence = probs.max().item()
    return {"prediction": pred, "confidence": round(confidence, 4)}
```

```dockerfile
# Dockerfile
FROM pytorch/pytorch:2.0.1-cuda11.7-cudnn8-runtime
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

### Day 26 — Large Language Model Fine-Tuning (PEFT / LoRA)

```python
from peft import get_peft_model, LoraConfig, TaskType
from transformers import AutoModelForCausalLM

# LoRA — Low-Rank Adaptation (fine-tune with <1% of parameters)
base_model = AutoModelForCausalLM.from_pretrained("meta-llama/Llama-2-7b-hf")

lora_config = LoraConfig(
    r=8,                        # Rank of decomposition
    lora_alpha=32,              # Scaling factor
    target_modules=["q_proj", "v_proj"],  # Which layers to adapt
    lora_dropout=0.1,
    task_type=TaskType.CAUSAL_LM
)

peft_model = get_peft_model(base_model, lora_config)
peft_model.print_trainable_parameters()
# Output: trainable params: 4,194,304 || all params: 6,742,609,920 || trainable%: 0.0622
```

#### PEFT Methods Comparison
| Method | Trainable Params | Memory | Quality |
|---|---|---|---|
| Full Fine-tune | 100% | Very High | Best |
| LoRA | 0.1-1% | Low | Excellent |
| Prefix Tuning | <1% | Low | Good |
| Prompt Tuning | <0.1% | Minimal | Moderate |

---

### Day 27 — Debugging & Profiling Deep Learning Models

```python
# 1. Gradient flow check
for name, param in model.named_parameters():
    if param.grad is not None:
        print(f"{name}: grad_norm = {param.grad.norm():.4f}")

# 2. Detect NaN gradients
def check_gradients(model):
    for name, param in model.named_parameters():
        if param.grad is not None and torch.isnan(param.grad).any():
            print(f"NaN gradient in {name}")

# 3. PyTorch Profiler
from torch.profiler import profile, record_function, ProfilerActivity

with profile(activities=[ProfilerActivity.CPU, ProfilerActivity.CUDA],
             record_shapes=True, profile_memory=True) as prof:
    with record_function("model_inference"):
        output = model(input_data)

print(prof.key_averages().table(sort_by="cuda_time_total", row_limit=10))

# 4. TensorBoard
from torch.utils.tensorboard import SummaryWriter
writer = SummaryWriter("runs/experiment_1")
writer.add_graph(model, input_data)
writer.add_scalar("Loss/train", loss.item(), epoch)
writer.add_histogram("Weights/fc1", model.fc1.weight, epoch)
writer.close()
```

---

### Day 28 — Responsible AI, Fairness & Interpretability

```python
# SHAP — SHapley Additive exPlanations
import shap

explainer = shap.DeepExplainer(model, background_data)
shap_values = explainer.shap_values(test_data)
shap.summary_plot(shap_values, test_data, feature_names=feature_names)

# Grad-CAM — visual explanation for CNNs
class GradCAM:
    def __init__(self, model, target_layer):
        self.model = model
        self.gradients = None
        self.activations = None
        target_layer.register_forward_hook(self._save_activation)
        target_layer.register_full_backward_hook(self._save_gradient)

    def _save_activation(self, module, input, output):
        self.activations = output

    def _save_gradient(self, module, grad_input, grad_output):
        self.gradients = grad_output[0]

    def generate(self, input_img, class_idx):
        output = self.model(input_img)
        self.model.zero_grad()
        output[0, class_idx].backward()
        weights = self.gradients.mean(dim=(2, 3), keepdim=True)
        cam = (weights * self.activations).sum(dim=1, keepdim=True)
        return torch.relu(cam)
```

#### Responsible AI Checklist
- [ ] Evaluate model on demographic subgroups separately
- [ ] Check for data leakage and target leakage
- [ ] Test for distribution shift between train and production
- [ ] Document model cards (intended use, limitations, training data)
- [ ] Implement input validation and output sanitization in API

---

### Day 29 — Capstone Project Day 1: Planning & Setup

#### 🎯 Choose Your Project

| Domain | Project Idea | Models to Use |
|---|---|---|
| NLP | Sentiment analysis / News classifier | BERT fine-tune |
| Vision | Custom image classifier / Object detection | ResNet + Transfer Learning |
| Generative | Text-to-image prompt explorer | Stable Diffusion API |
| Multimodal | Image captioning system | CLIP + GPT-2 |
| Time Series | Stock price / demand forecasting | LSTM / Transformer |

#### Capstone Deliverables
- [ ] Clean dataset with train/val/test splits
- [ ] Model architecture defined and documented
- [ ] Training loop with loss/accuracy tracking (WandB)
- [ ] Baseline model and improved model comparison
- [ ] REST API serving the model (FastAPI)
- [ ] Dockerfile for containerization
- [ ] README with results and architectural decisions

---

### Day 30 — Capstone Completion, Reflection & Next Steps

#### 🎯 Finalize Your Capstone
- Train final model, document results
- Deploy API and test with Postman / curl
- Write a 1-page technical writeup

#### 📊 What to Document
```markdown
## Model Card

### Intended Use
- Primary purpose: Image classification for e-commerce product sorting
- Out-of-scope: Medical imaging, real-time video

### Dataset
- Training: 50,000 images (ImageNet subset)
- Validation: 10,000 images
- Test: 10,000 images

### Performance
| Metric | Value |
|---|---|
| Top-1 Accuracy | 94.2% |
| Top-5 Accuracy | 99.1% |
| Inference Latency | 12ms (GPU) / 85ms (CPU) |
| Model Size | 98MB (INT8 quantized: 25MB) |

### Known Limitations
- Accuracy drops to 87% on low-resolution images (<64px)
- Poor performance on occluded objects
```

#### Your Learning Journey Continues

| Next Step | Resources |
|---|---|
| Read papers | arXiv.org — cs.LG, cs.CV, cs.CL sections |
| Reproduce papers | [Papers with Code](https://paperswithcode.com/) |
| Kaggle competitions | Start with bronze, aim for silver |
| LLM specialization | Hugging Face Course (free) |
| MLOps deep dive | Made With ML, Full Stack Deep Learning |
| Research path | Attend NeurIPS / ICML workshops |

---

## 📊 30-Day Summary

| Week | Days | Focus |
|---|---|---|
| **Week 1** | 1–7 | Math foundations, ML review, NumPy NN, PyTorch basics |
| **Week 2** | 8–14 | Activations, optimizers, regularization, CNNs, RNNs, Transformers |
| **Week 3** | 15–21 | ResNet, BERT, GPT, ViT, GANs, Diffusion models, RL intro |
| **Week 4** | 22–30 | MLOps, deployment, quantization, LoRA, debugging, capstone |

---

## ⚡ Must-Know Paper Checklist

| Paper | Year | Contribution |
|---|---|---|
| Attention Is All You Need | 2017 | Transformer architecture |
| BERT | 2018 | Bidirectional language model |
| GPT-3 | 2020 | Few-shot learning at scale |
| ResNet | 2015 | Residual connections |
| DDPM | 2020 | Denoising Diffusion Models |
| LoRA | 2021 | Efficient fine-tuning |
| CLIP | 2021 | Vision-language pretraining |
| InstructGPT | 2022 | RLHF for alignment |

---

## 📖 Resources

| Type | Resource |
|---|---|
| Course | [fast.ai Practical DL](https://course.fast.ai/) — top-down, practical |
| Course | [DeepLearning.AI Specialization](https://www.coursera.org/specializations/deep-learning) |
| Book | *Deep Learning* by Goodfellow, Bengio, Courville (free online) |
| Book | *Dive into Deep Learning* — d2l.ai (interactive, PyTorch) |
| Papers | [arXiv cs.LG](https://arxiv.org/list/cs.LG/recent) |
| Practice | [Kaggle DL competitions](https://www.kaggle.com/competitions) |
| Models | [Hugging Face Hub](https://huggingface.co/models) |
| Tracking | [Weights & Biases](https://wandb.ai/) |

---

## ⚠️ Common Deep Learning Pitfalls

1. **Not normalizing inputs** — always standardize to zero mean, unit variance
2. **Learning rate too high/low** — the most impactful hyperparameter; always tune first
3. **Overfitting with no validation split** — always monitor val loss, not just train loss
4. **Training in `model.train()` during eval** — always call `model.eval()` for inference
5. **Forgetting `optimizer.zero_grad()`** — gradients accumulate and corrupt training
6. **Using CPU when GPU is available** — always `.to(device)` tensors and model together
7. **No gradient clipping for RNNs/LLMs** — use `torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)`
8. **Ignoring class imbalance** — use weighted loss or oversampling
9. **Data leakage** — normalize with training set stats only; apply to val/test
10. **Not saving checkpoints** — always save best model periodically

---

*From weights and biases to production pipelines — you've got this! 🚀*
