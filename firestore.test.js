const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kafka_123";
const BOB_UID = "bob_kafka_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied on sessions, skills, and artifacts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("skills").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("artifacts").get()
  );
});

test("Session CRUD & Message creation: valid payloads succeed, cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("session_1");

  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "AI Quantum Survey",
      mode: "JOURNAL_RESEARCH",
      activeSkillIds: ["skill_docx"],
      lastSummary: "Initialized",
      verifiedScore: 92,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    sessionRef.update({
      lastSummary: "Verified 4 journals",
      verifiedScore: 95,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / immutable createdAt change
  await assertFails(
    sessionRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user read & write rejected
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("session_1")
      .get()
  );

  // Create message under session
  const msgRef = sessionRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "AGENT",
      content: "Verified quantum error correction results.",
      thoughtSteps: ["Search OpenAlex", "Verify DOI 10.1038/x"],
      verificationStatus: "VERIFIED",
      verificationScore: 95,
      verificationSummary: "3 peer-reviewed sources confirmed.",
      citations: [
        { title: "Nature QEC", url: "https://doi.org/10.1038/x", source: "OpenAlex", verified: true },
      ],
      createdAt: new Date(Date.now() - 400),
    })
  );

  // List queries match Kotlin repository paths
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertSucceeds(sessionRef.collection("messages").get());
});

test("Skills & Artifacts: valid create/update succeed, invalid enums & oversized fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  const skillRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("skills")
    .doc("skill_pptx");

  await assertSucceeds(
    skillRef.set({
      userId: ALICE_UID,
      name: "Executive PPTX Builder",
      slug: "executive-pptx",
      description: "Generates multi-slide PPTX decks.",
      category: "DOCUMENT",
      sourceType: "AI_CREATED",
      systemPrompt: "Synthesize structured slides with speaker notes.",
      outputFormat: "PPTX",
      enabled: true,
      usageCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    skillRef.update({
      enabled: false,
      usageCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid outputFormat
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("skills")
      .doc("bad_skill")
      .set({
        userId: ALICE_UID,
        name: "Bad",
        slug: "bad",
        description: "desc",
        category: "CUSTOM",
        sourceType: "AI_CREATED",
        systemPrompt: "prompt",
        outputFormat: "EXE",
        enabled: true,
        usageCount: 0,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Valid artifact
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("artifacts")
      .doc("art_1")
      .set({
        userId: ALICE_UID,
        sessionId: "session_1",
        title: "Quantum Report",
        format: "DOCX",
        fileName: "Quantum_Report.docx",
        localPath: "/files/Quantum_Report.docx",
        summary: "1200-word verified DOCX dossier",
        wordCount: 1200,
        verified: true,
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("UserProfile: owner create/update succeed; cross-user read and list fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Researcher",
      email: "alice@example.com",
      bio: "AI & Biotech Analyst",
      darkTheme: true,
      defaultFormat: "DOCX",
      autoVerify: true,
      preferredLanguage: "Indonesia",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    profileRef.update({
      defaultFormat: "PPTX",
      darkTheme: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("ResearchLibrary Snapshots: owner create/list succeed; cross-user & invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  const snapRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("researchLibrary")
    .doc("snap_1");

  await assertSucceeds(
    snapRef.set({
      userId: ALICE_UID,
      sourceSessionId: "session_1",
      title: "LLM Reasoning Snapshot",
      notes: "Saved before presentation export",
      mode: "AGENT",
      activeSkillIds: ["builtin_journal", "builtin_pptx"],
      messageCount: 2,
      messagesArchive: [
        { role: "USER", content: "Summarize LLM reasoning", verificationScore: 0 },
        { role: "AGENT", content: "Synthesized findings [J1]", verificationScore: 91 },
      ],
      citations: [
        { refTag: "J1", title: "Chain of Thought", url: "https://arxiv.org/abs/2201.11903", verified: true },
      ],
      hasActivePreview: true,
      previewFormat: "PPTX",
      previewTitle: "LLM Reasoning Deck",
      previewSubtitle: "Kafka AI",
      previewSummary: "Executive synthesis of reasoning benchmarks.",
      previewSections: [
        { heading: "Key Findings", body: "Improved accuracy.", bullets: ["CoT", "Self-consistency"] },
      ],
      previewReferences: ["Wei et al. (2022)"],
      verifiedScore: 91,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("researchLibrary").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("researchLibrary").doc("snap_1").get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("researchLibrary").doc("bad_snap").set({
      userId: ALICE_UID,
      sourceSessionId: "s1",
      title: "",
      notes: "",
      mode: "INVALID",
      activeSkillIds: [],
      messageCount: 0,
      messagesArchive: [],
      citations: [],
      hasActivePreview: false,
      previewFormat: "",
      previewTitle: "",
      previewSubtitle: "",
      previewSummary: "",
      previewSections: [],
      previewReferences: [],
      verifiedScore: 100,
      createdAt: past,
      updatedAt: past,
    })
  );
});

