import { useState, useEffect } from "react";

const initialContacts = [
  { id: 1, name: "John", surname: "Doe", phone: "123-456-789" },
  { id: 2, name: "Jane", surname: "Smith", phone: "987-654-321" },
  { id: 3, name: "Alice", surname: "Johnson", phone: "555-123-456" },
];



function App() {
  const [contacts, setContacts] = useState(() => {
    const savedContacts = localStorage.getItem("contacts");
    return savedContacts ? JSON.parse(savedContacts) : initialContacts;
  });

  const [name, setName] = useState("");
  const [surname, setSurname] = useState("");
  const [phone, setPhone] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState("name");
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState("");
  const [editSurname, setEditSurname] = useState("");
  const [editPhone, setEditPhone] = useState("");

  // Save to localStorage whenever contacts change
  useEffect(() => {
    localStorage.setItem("contacts", JSON.stringify(contacts));
  }, [contacts]);

  function handleSubmit(e) {
    e.preventDefault();
    if (!name || !surname || !phone) {
      alert("Please fill all fields");
      return;
    }
    const newContact = { id: Date.now(), name, surname, phone };
    setContacts([...contacts, newContact]);
    setName("");
    setSurname("");
    setPhone("");
  }

  function deleteContact(id) {
    if (window.confirm("Are you sure you want to delete this contact?")) {
      setContacts(contacts.filter((item) => item.id !== id));
    }
  }

  function startEdit(contact) {
    setEditingId(contact.id);
    setEditName(contact.name);
    setEditSurname(contact.surname);
    setEditPhone(contact.phone);
  }

  function saveEdit(id) {
    setContacts(contacts.map(contact =>
        contact.id === id
            ? { ...contact, name: editName, surname: editSurname, phone: editPhone }
            : contact
    ));
    setEditingId(null);
  }

  function cancelEdit() {
    setEditingId(null);
  }

  // Filter contacts based on search
  const filteredContacts = contacts.filter(contact => {
    const fullName = `${contact.name} ${contact.surname}`.toLowerCase();
    const phoneNumber = contact.phone.toLowerCase();
    const searchLower = searchTerm.toLowerCase();
    return fullName.includes(searchLower) || phoneNumber.includes(searchLower);
  });

  // Sort contacts
  const sortedContacts = [...filteredContacts].sort((a, b) => {
    if (sortBy === "name") {
      return a.name.localeCompare(b.name);
    } else if (sortBy === "surname") {
      return a.surname.localeCompare(b.surname);
    }
    return 0;
  });

  return (
      <div className="app-container">
        <h1>📒 Contact List</h1>

        {/* Search Input */}
        <div className="search-container">
          <input
              type="text"
              placeholder="🔍 Search by name or phone..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
          />
        </div>

        {/* Sort Dropdown */}
        <div className="sort-container">
          <label>Sort by: </label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="name">First Name</option>
            <option value="surname">Last Name</option>
          </select>
        </div>

        {/* Add Contact Form */}
        <form className="contact-form" onSubmit={handleSubmit}>
          <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="First Name"
              required
          />
          <input
              value={surname}
              onChange={(e) => setSurname(e.target.value)}
              placeholder="Last Name"
              required
          />
          <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="Phone Number"
              required
          />
          <button type="submit">Add Contact</button>
        </form>

        {/* Contacts List */}
        {sortedContacts.length === 0 ? (
            <p className="no-contacts">No contacts found</p>
        ) : (
            <ul className="contact-list">
              {sortedContacts.map((item) => (
                  <Contact
                      key={item.id}
                      id={item.id}
                      name={item.name}
                      surname={item.surname}
                      phone={item.phone}
                      onDelete={deleteContact}
                      onEdit={startEdit}
                      isEditing={editingId === item.id}
                      editName={editName}
                      editSurname={editSurname}
                      editPhone={editPhone}
                      setEditName={setEditName}
                      setEditSurname={setEditSurname}
                      setEditPhone={setEditPhone}
                      onSave={saveEdit}
                      onCancel={cancelEdit}
                  />
              ))}
            </ul>
        )}

        <div className="stats">
          Total contacts: {sortedContacts.length}
        </div>
      </div>
  );
}

function Contact({
                   id, name, surname, phone, onDelete, onEdit, isEditing,
                   editName, editSurname, editPhone, setEditName, setEditSurname,
                   setEditPhone, onSave, onCancel
                 }) {
  if (isEditing) {
    return (
        <li className="contact-item editing">
          <div className="edit-fields">
            <input
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                placeholder="First Name"
                className="edit-input"
            />
            <input
                value={editSurname}
                onChange={(e) => setEditSurname(e.target.value)}
                placeholder="Last Name"
                className="edit-input"
            />
            <input
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
                placeholder="Phone"
                className="edit-input"
            />
          </div>
          <div className="contact-actions">
            <button onClick={() => onSave(id)} className="save">💾 Save</button>
            <button onClick={onCancel} className="cancel">❌ Cancel</button>
          </div>
        </li>
    );
  }

  return (
      <li className="contact-item">
      <span>
        <strong>{name} {surname}</strong> — {phone}
      </span>
        <div className="contact-actions">
          <button onClick={() => onEdit({ id, name, surname, phone })} className="edit">
            ✏️ Edit
          </button>
          <button onClick={() => onDelete(id)} className="delete">
            🗑️ Delete
          </button>
        </div>
      </li>
  );
}

export default App;