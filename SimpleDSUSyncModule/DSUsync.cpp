// correct way to include ndn-cxx headers
// #include <ndn-cxx/face.hpp>
// #include <ndn-cxx/util/scheduler.hpp>
#include <string>
#include "security/key-chain.hpp"
#include "face.hpp"
#include "util/scheduler.hpp"

// Enclosing code in ndn simplifies coding (can also use `using namespace ndn`)
namespace ndn {
// Additional nested namespace could be used to prevent/limit name contentions
namespace examples {

  static const std::string GROUPED_DATA_PREFIX = "/org/openmhealth/haitao/grouped/walking";
  static const std::string ORIGINAL_DATA_PREFIX = "/org/openmhealth/haitao/raw/walking";
  static const std::string CONFIRM_DATA_PREFIX = "/org/openmhealth/haitao/confirm/walking";
  static const std::string CONFIRM_CONTENT = "confirm";

class DSUsync : noncopyable
{
public:
  DSUsync()
    : m_face(m_ioService) // Create face with io_service object
    , m_scheduler(m_ioService)
  {
  }

  void
  run()
  {
    m_face.setInterestFilter(CONFIRM_DATA_PREFIX,
                             bind(&DSUsync::onInterest, this, _1, _2),
                             RegisterPrefixSuccessCallback(),
                             bind(&DSUsync::onRegisterFailed, this, _1, _2));

    Interest interest(Name(GROUPED_DATA_PREFIX).appendSequenceNumber(seqNo++));
    interest.setInterestLifetime(time::seconds(1));
    interest.setMustBeFresh(true);
    m_face.expressInterest(interest,
                           bind(&DSUsync::onData, this, _1, _2),
                           bind(&DSUsync::onTimeout, this, _1));

    std::cout << "Sending " << interest << std::endl;

    // m_ioService.run() will block until all events finished or m_ioService.stop() is called
    m_ioService.run();

    // Alternatively, m_face.processEvents() can also be called.
    // processEvents will block until the requested data received or timeout occurs.
    // m_face.processEvents();
  }

private:
  void
  onData(const Interest& interest, const Data& data)
  {
    //    std::cout << data << std::endl;
    if(interest.getName().toUri().find("grouped")!=std::string::npos) {
      // Produce data for future confirmation
      shared_ptr<Data> confirmData = make_shared<Data>();
      confirmData->setName(Name(CONFIRM_DATA_PREFIX).append(interest.getName().get(-1)));
      confirmData->setContent(reinterpret_cast<const uint8_t*>(CONFIRM_CONTENT.c_str()), CONFIRM_CONTENT.size());
      // Sign Data packet with default identity
      m_keyChain.sign(*confirmData);
      confirmMap.insert({confirmData->getName(), confirmData});

      std::string content((char *)data.getContent().value(), data.getContent().value_size());
      //std::cout << content << std::endl;

      char * cstr = new char [content.length() + 1];
      std::strcpy (cstr, content.c_str());
      // cstr now contains a c-string copy of str
      char * originalDataNameStr = std::strtok (cstr,"##");
      while (originalDataNameStr!=0)
      {
      //  std::cout << originalDataNameStr << std::endl;
        Name originalDataName(originalDataNameStr);
        Interest originalDataInterest(originalDataName);
        originalDataInterest.setMustBeFresh(true);
        originalDataInterest.setInterestLifetime(time::seconds(1));
        m_face.expressInterest(originalDataInterest,
                           bind(&DSUsync::onData, this, _1, _2),
                           bind(&DSUsync::onTimeout, this, _1));    
        //  std::cout << "Sending " << originalDataInterest << std::endl;
        originalDataNameStr = std::strtok(NULL, "##");
      }
      delete[] cstr;

      Interest nextGroupedInterest(Name(GROUPED_DATA_PREFIX).appendSequenceNumber(seqNo++));
      nextGroupedInterest.setMustBeFresh(true);
      nextGroupedInterest.setInterestLifetime(time::seconds(1));
      m_face.expressInterest(nextGroupedInterest,
                           bind(&DSUsync::onData, this, _1, _2),
                           bind(&DSUsync::onTimeout, this, _1));    
      std::cout << "Sending " << nextGroupedInterest << std::endl;
    }
  }

  void
  onInterest(const InterestFilter& filter, const Interest& interest)
  {
    std::cout << "<< I: " << interest << std::endl;

    // Create new name, based on Interest's name
    Name confirmDataName(interest.getName());

    std::map<Name,shared_ptr<Data>>::iterator it;
    it = confirmMap.find(confirmDataName);
    if (it != confirmMap.end()) {
      shared_ptr<Data> data = it->second;

      // Return Data packet to the requester
      std::cout << ">> D: " << *data << std::endl;
      m_face.put(*data);
    }
  }

  void
  onTimeout(const Interest& interest)
  {
    std::cout << "Timeout " << interest << std::endl;
  }

  void
  onRegisterFailed(const Name& prefix, const std::string& reason)
  {
    std::cerr << "ERROR: Failed to register prefix \""
              << prefix << "\" in local hub's daemon (" << reason << ")"
              << std::endl;
  }

private:
  // Explicitly create io_service object, which can be shared between Face and Scheduler
  boost::asio::io_service m_ioService;
  Face m_face;
  Scheduler m_scheduler;
  uint64_t seqNo = 0;
  std::map<Name, std::shared_ptr<Data>> confirmMap;
  KeyChain m_keyChain;
};



} // namespace examples
} // namespace ndn

int
main(int argc, char** argv)
{
  ndn::examples::DSUsync dsusync;
  try {
    dsusync.run();
  }
  catch (const std::exception& e) {
    std::cerr << "ERROR: " << e.what() << std::endl;
  }
  return 0;
}
