import idx2numpy

TRAIN_IMAGE_FILENAME= '/Users/dan/Downloads/MNIST_ORG/train-images.idx3-ubyte'
TRAIN_LABEL_FILENAME= '/Users/dan/Downloads/MNIST_ORG/train-labels.idx1-ubyte'
TEST_IMAGE_FILENAME= '/Users/dan/Downloads/MNIST_ORG/t10k-images.idx3-ubyte'
TEST_LABEL_FILENAME= '/Users/dan/Downloads/MNIST_ORG/t10k-labels.idx1-ubyte'

# Read files
train_images = idx2numpy.convert_from_file(TRAIN_IMAGE_FILENAME)
train_labels = idx2numpy.convert_from_file(TRAIN_LABEL_FILENAME)

test_images = idx2numpy.convert_from_file(TEST_IMAGE_FILENAME)
test_labels = idx2numpy.convert_from_file(TEST_LABEL_FILENAME)

# Print dimensions
print('dimensions of train_images:', train_images.shape)
print('dimensions of train_labels:', train_labels.shape)
print('dimensions of test_images:', test_images.shape)
print('dimensions of test_labels:', test_labels.shape)

print('label for first training example: ', train_labels[0])
print('---beginning of pattern for first training example---')
for line in train_images[0]:
    for num in line:
        if num > 0:
            print('*', end='')
        else:
            print(' ', end=' ')
    print('')
print('---end of pattern for first training example---')
