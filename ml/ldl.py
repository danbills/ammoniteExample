import numpy as np

np.random.seed(9) # to make repeatable

LEARNING_RATE = 0.1

index_list = [0, 1, 2, 3]

# Define training examples for XOR

# first number in each array is the bias
x_train = [np.array([1.0, -1.0, -1.0]), # two negatives
              np.array([1.0, -1.0, 1.0]), # negative, positive
              np.array([1.0, 1.0, -1.0]), # positive, negative
              np.array([1.0, 1.0, 1.0])] # two positives

y_train = [0.0, 1.0, 1.0, 0.0] # XOR

def neuron_w(input_count):
    weights = np.zeros(input_count+1)
    for i in range(1, (input_count + 1)):
        weights[i] = np.random.uniform(-1.0, 1.0)
    return weights

n_w = [neuron_w(2), neuron_w(2), neuron_w(2)]

n_y = [0,0,0]

n_error = [0,0,0]

def show_learning():
    print('Current weights:')
    for i, w in enumerate(n_w):
        print('neuron ', ': w0 =', '%5.2f' % w[0], 'w1 =', '%5.2f' % w[1], 'w2 =', '%5.2f' % w[2])
    print('-----------------------------------')

def forward_pass(x):
    global n_y
    n_y[0] = np.tanh(np.dot(n_w[0], x)) # Neuron 0
    n_y[1] = np.tanh(np.dot(n_w[1], x)) # Neuron 1
    n2_inputs = np.array([1.0, n_y[0], n_y[1]]) # 1.0 is the bias
    z2 = np.dot(n_w[2], n2_inputs) # the dot product is the sum of the weighted inputs
    n_y[2] = 1.0 / (1.0 + np.exp(-z2)) # Neuron 2. Exp calculates the exponential of all elements in the input array.  
    # Exponential function is e^x where e is Euler's number (approximately equal to 2.71828) and x is the number passed to it.

def backward_pass(y_truth):
    global n_error
    error_prime = -(y_truth - n_y[2]) # Derivative of the error or loss function

    derivative = n_y[2] * (1.0 - n_y[2]) # Derivative of the logistic (sigmoid) function
    n_error[2] = error_prime * derivative # Error for Neuron 2

    derivative = 1.0 - n_y[0]**2 # Derivative of the tanh function
    n_error[0] = n_w[2][1] * n_error[2] * derivative # Error for Neuron 0

    derivative = 1.0 - n_y[1]**2 # Derivative of the tanh function
    n_error[1] = n_w[2][2] * n_error[2] * derivative # Error for Neuron 1

def adjust_weights(x):
    global n_w
    n_w[0] -= (x * LEARNING_RATE * n_error[0]) # weight 0 i adjusted by: the error * the learning rate * the input
    n_w[1] -= (x * LEARNING_RATE * n_error[1]) 
    n2_inputs = np.array([1.0, n_y[0], n_y[1]]) # 1.0 is the bias
    n_w[2] -= (n2_inputs * LEARNING_RATE * n_error[2]) 

# Network training loop
all_correct = False
num_iterations = 0
while not all_correct:
    num_iterations += 1
    all_correct = True
    np.random.shuffle(index_list) # Randomize the order of the training examples
    for i in index_list:
        forward_pass(x_train[i])
        backward_pass(y_train[i])
        adjust_weights(x_train[i])
        show_learning()
    for i in range(len(x_train)):
        forward_pass(x_train[i])
        print('x1 =', '%4.1f' % x_train[i][1], 'x2 =', '%4.1f' % x_train[i][2], 'y_truth =', '%4.1f' % y_train[i], 'y =', '%6.3f' % n_y[2])
        if (((y_train[i] < 0.5) and (n_y[2] >= 0.5)) or ((y_train[i] >= 0.5) and (n_y[2] < 0.5))):
            all_correct = False

print('Number of iterations:', num_iterations)
