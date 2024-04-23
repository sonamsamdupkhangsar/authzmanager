package me.sonam.authzmanager.controller.clients;

import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.controller.clients.carrier.User;
import me.sonam.authzmanager.controller.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.clients.carrier.ClientOrganizationUserWithRole2;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientControllerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientControllerTest.class);

    @Test
    void testContains1() {
        List<me.sonam.authzmanager.clients.user.User> usersInOrganizationList = List.of(new me.sonam.authzmanager.clients.user.User(UUID.fromString("e0fd7c33-2d10-4edf-9ff9-ea04d5b3cd3c"),
                "test3@sonam.email"),
                new me.sonam.authzmanager.clients.user.User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), "me@sonam.email"));

        List<ClientOrganizationUserWithRole> clientOrganizationUserWithRoleList = List.of(new
                ClientOrganizationUserWithRole(UUID.fromString("3c0836c5-ada7-4fd9-a6d3-feaf36981d51"),
                UUID.fromString("0520534c-ce5f-4a25-900d-37621ebce3a3"),
                new User(UUID.fromString("e0fd7c33-2d10-4edf-9ff9-ea04d5b3cd3c"),
                        new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"),
                                UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25")))));//objects.getT1();

        LOG.info("get users (id not in) that are not in clientOrganizationUseWithRole");

        assertFalse(clientOrganizationUserWithRoleList.contains(UUID.randomUUID()));

        Map<UUID, me.sonam.authzmanager.clients.user.User> map = new HashMap<>();
        for(me.sonam.authzmanager.clients.user.User user: usersInOrganizationList) {
            map.put(user.getId(), user);
        }


        //if the user in clientOrganizationUserWithRoleList  exists in the usersInOrganiationList then remove that from the usersInOrganizationList
        //List<User> userWithNoRole = clientOrganizationUserWithRoleList.stream().filter(clientOrganizationUserWithRole -> )

        // List<User> userNotInClientOrganziationRoleList = usersInOrganizationList.stream().filter(user -> !clientOrganizationUserWithRoleList.contains(user.getId())).toList();

        LOG.info("cilentOrganizationUserWithRole.size: {}", clientOrganizationUserWithRoleList.size());

        LOG.info("before filtering usersInOrganizationList.size: {}", usersInOrganizationList.size());

        LOG.info("usersInOrganizationList: {}", usersInOrganizationList);

        List<me.sonam.authzmanager.clients.user.User> clientOrgUserNotInClientOrganziationRoleList = usersInOrganizationList.stream().filter(user -> !clientOrganizationUserWithRoleList.contains(user.getId())).toList();

        LOG.info("userNotInClientOrganziationRoleList: {}", clientOrgUserNotInClientOrganziationRoleList);

        Map<UUID, me.sonam.authzmanager.clients.user.User> userMap = new HashMap<>();
        List<me.sonam.authzmanager.clients.user.User> userList = usersInOrganizationList;
        LOG.info("before removing userList contains: {}", userList);

        userList.forEach(clientOrgUser -> userMap.put(clientOrgUser.getId(), clientOrgUser));
        LOG.info("userMap contains: {}", userMap);

        LOG.info("userNotInClientOrganziationRoleList.size: {}", clientOrgUserNotInClientOrganziationRoleList.size());


    }

    @Test
    public void testContains() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();

        List<MyUser> list = List.of(new MyUser(uuid1), new MyUser(uuid2), new MyUser(uuid3));

        LOG.info("list contains: {}", list);

        LOG.info("list.contains myuser uuid2: {}", list.contains(new MyUser(uuid2)));
        assertTrue(list.contains(new MyUser(uuid2)));

        LOG.info("list.contains myuser uuid4: {}", list.contains(new MyUser(uuid4)));
        assertFalse(list.contains(new MyUser(uuid4)));

    }
    @Test
    public void client2() {
        LOG.info("test clientOrganizationUserWithRole2 classes");
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();


        List<ClientOrganizationUserWithRole2> list = List.of(new ClientOrganizationUserWithRole2(uuid1), new ClientOrganizationUserWithRole2(uuid2), new ClientOrganizationUserWithRole2(uuid3));

        LOG.info("list contains: {}", list);
        LOG.info("list.contains myuser uuid2: {}", list.contains(new ClientOrganizationUserWithRole2(uuid2)));
        assertTrue(list.contains(new ClientOrganizationUserWithRole2(uuid2)));

        LOG.info("list.contains myuser uuid4: {}", list.contains(new ClientOrganizationUserWithRole2(uuid4)));
        assertFalse(list.contains(new ClientOrganizationUserWithRole2(uuid4)));

        LOG.info("this should be true");
        ClientOrganizationUserWithRole2 cour = new ClientOrganizationUserWithRole2(uuid2);
        assertTrue(list.contains(cour));

        LOG.info("this should be false");
        cour.setOrganizationId(UUID.randomUUID());
        assertFalse(list.contains(cour));
    }

    @Test
    public void testRole2() {
        UUID user1 = UUID.fromString("e0fd7c33-2d10-4edf-9ff9-ea04d5b3cd3c");

        ClientOrganizationUserWithRole2 cour1 = new
                ClientOrganizationUserWithRole2(UUID.fromString("3c0836c5-ada7-4fd9-a6d3-feaf36981d51"),
                UUID.fromString("0520534c-ce5f-4a25-900d-37621ebce3a3")
                , new User(user1,
                        new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"),
                                UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"))));

        ClientOrganizationUserWithRole2 cour2 = new
                ClientOrganizationUserWithRole2(UUID.fromString("3c0836c5-ada7-4fd9-a6d3-feaf36981d51"),
                UUID.fromString("0520534c-ce5f-4a25-900d-37621ebce3a3"),
                new User(UUID.randomUUID(),
                        new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"),
                                UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"))));

        ClientOrganizationUserWithRole2 cour3 = new
                ClientOrganizationUserWithRole2(UUID.fromString("3c0836c5-ada7-4fd9-a6d3-feaf36981d51"),
                UUID.fromString("0520534c-ce5f-4a25-900d-37621ebce3a3"),
                new User(UUID.randomUUID(),
                        new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"),
                                UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"))));


        List<ClientOrganizationUserWithRole2> list = List.of(cour1, cour2, cour3);

        //list.contains(user1);
        LOG.info("assert list contains");
        assertTrue(list.contains(new ClientOrganizationUserWithRole2(UUID.fromString("3c0836c5-ada7-4fd9-a6d3-feaf36981d51"), UUID.fromString("0520534c-ce5f-4a25-900d-37621ebce3a3"))));
       // assertFalse(list.contains(UUID.randomUUID()));

    }




    class MyUser {
        private UUID id;
        public MyUser(UUID id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object object) {
            LOG.info("equals being called");
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            MyUser myUser = (MyUser) object;
            return Objects.equals(id, myUser.id);
        }

        @Override
        public int hashCode() {
            LOG.info("hashcode being called");
            return Objects.hashCode(id);
        }

        @Override
        public String toString() {
            return "MyUser{" +
                    "id=" + id +
                    '}';
        }
    }


}