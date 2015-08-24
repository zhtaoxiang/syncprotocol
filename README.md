1. How to run the code?
  This code can only be run on the Linux now. To run it on a local machine, 
follow the steps.
  (1) run NFD;
  (2) use NetBeans (or other IDEs) to run the main function in NetworkConnection 
class;
  (3) put DSUsync.cpp file under the "examples" directory of ndn-cxx project, 
follow https://github.com/cawka/ndn-cxx/blob/master/docs/INSTALL.rst this to
build and run the example.

2. Some explanations
  In this simple demo, an in-memory repo is used to store the local data, and 
all the data are faked by DataFaker class.

3. What to do next
  (1) integrate this module with the NDNFit Android application;
  (2) design an good strategy to delete the local data.