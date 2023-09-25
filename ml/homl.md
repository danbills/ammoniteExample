## Types of machine learning systems

Criteria are:

- How they are supervised
  - supervised
  - unsupervised
  - semi-supervised
  - self-supervised
  - others
- Whether they can learn incrementally on the fly 
  - online 
  - batch
    - 




### Supervised

The trainig set fed in is "labeled."  I.e. the "desired solution" is accompanied with the data points.

#### Classification

- e.g. 
  - Spam Filter 

#### Regression

- Predict a target numeric value
  - Car price iven a set of features

### Unsupervised

Data is unlabeled

#### Clustering

*Hierarchical Clustering*
try to detect clusters and subdivide gropus into smaller groups.

*Visualization Algorithms*
Output a 2D or 3D representation of data that can be easily plotted.

#### Dimensionality reduction

Simplify the data without losing too much useful information.

##### merge several correlated features into one

E.g. mileage may be correlated with age, so merge them into one feature that represents a car's wear and tear.

This is called *feature extraction.*

#### Anomaly detection

*Novelty detection*
detect new instances that look different from all instances in the training set.  Require a very clean training set.

#### Association rule learning

Dig into large amounts of data dn discover interesting relations between attributes.

### Semi-supervised learning

Some algorithms can deal with data that's partially labeled.
E.g. Google photos: recognizes people, then you simply label 1 and it can find all the rest and label those too

Usually a combination of unsupervised and supervised algorithms.

### Self-supervised learning

generate a fully labeled dataset from a fully unlabeled one

e.g. large dataset of unlabeled images
- randomly mask a smallr part of each image
- train a model to recover the original image
- masked images are the inputs, original images are the labels

### Reinforcement learning

- Learning system known as "agent" 
- observe the environment, select and perform actions
- get rewards or penalties

e.g. robots learn how to walk 
e.g. DeepMind beatin Go grandmaster

## Batch vs. Online learning

### Batch 

Considered "offline" learning.  Trained using all available data. 
- Suffers from *data drift* or *model rot*

### Online learning

Train the system incrementally
- Feed it data instances sequentially
  - individually 
  - small groups called *mini-batches*
- Good for 
  - systems that need to adapt extremely rapidly
  - limited computing resources (eg mobile device)
  - Running large datasets that don't fit in memory
- *Learning rate*
  - How fast it should adapt to changing data
  - High learning rate
    - rapidly adapt to new data
    - may quickly forget older data
  - Low learning rate
    - More inertia
    - Learn more slowly
    - Less sensitive to noise

## Instance-based vs Model-based learning

How do machine learning systems *_generalize_*

*generalize*
System needs to be able to make good predictions for (_generalize_ to) examples it has not seen before.  The true goal is to perform well on new instances.

### Instance-based learning

- memorize everything "by heart"
- *measure of similarity*
  - e.g. count number of words spam emails have in common
- generalizes to new cases by using a similarity measure to compare them to the learned examples

### Model-based learning

E.g. linear model  life_satisfaction = theta0 + theta1*GDP_per_capita

- Performance measures
    - utility function (or _fitness function_)
        - measures how good the model is
    - cost function
      - measures how _bad_ the model is
      - for linear regression, use a cost function that measures the distance between the linear model's predictions and the training examples, and try to minimize this distance
- choosing linear model is called *model selection*




# Questions

1. How would you define machine learning?

Making a machine understand enough data to make useful predictions of answers to new questions.

2. Can you name four types of applications where it shines?

1. spam filter
2. speech recognition
3. cancer detection
4. image object recognition

3. What is a labeled training set?

A labeled training set accompanies data with labels (a.k.a. "meta"-data) that adds useful information to the data point.

4. What are the two most common supervised tasks?



