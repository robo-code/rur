FROM zamboch/roborumble:latest

COPY ./build/libs/*.jar /root/robocode/libs/
COPY ./robocode.plugin.kusto/build/libs/*.jar /root/robocode/libs/
COPY ./robocode.plugin.kusto/robocode.properties /root/robocode/config/
COPY ./robocode.plugin.kusto/roborumble.sh /root/robocode/
RUN chmod a+x /root/robocode/roborumble.sh

ENV USER=zamboch
ENV PARTICIPANTSURL=https://raw.githubusercontent.com/pavelsavara/rumblelist/main/participants-roborumble-top20.txt
ENV UPDATEBOTSURL=https://robocode-306519.oa.r.appspot.com/RemoveOldParticipant
ENV RESULTSURL=https://robocode-306519.oa.r.appspot.com/UploadedResults
ENV RATINGSURL=https://robocode-306519.oa.r.appspot.com/RatingsFile

WORKDIR /root/robocode
ENTRYPOINT ["./roborumble.sh"]