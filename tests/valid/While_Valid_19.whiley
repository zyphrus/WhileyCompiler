

constant SIZE is 5

public export method test() :
    int[][] components = [[0;0]; SIZE]
    int i = 0
    while i < |components|:
        components[i] = [0]
        i = i + 1
    assume components == [[0],[0],[0],[0],[0]]
